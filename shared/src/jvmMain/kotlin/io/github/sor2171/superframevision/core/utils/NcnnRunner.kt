package io.github.sor2171.superframevision.core.utils

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import org.slf4j.LoggerFactory
import superframevision.shared.generated.resources.Res
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NcnnRunner(
    private val sourceSize: Pair<Int, Int>,
    private val modelNetPtr: Pointer,
    private val optionPtr: Pointer,
    private val extractorPtr: Pointer,
    private val pipelineCachePtr: Pointer,
    private val times: Int
) : AutoCloseable {

    private val targetWidth = ceilTo32(sourceSize.first)
    private val targetHeight = ceilTo32(sourceSize.second)
    private val elemsize = 4L


    actual companion object {
        private val logger = LoggerFactory.getLogger(this.javaClass)

        private val cLib: NcnnLibrary by lazy {
            val libAbsolutePath = NcnnLoader.loadNcnn()
            val instance = Native.load(libAbsolutePath, NcnnLibrary::class.java)
            instance
        }

        actual fun listVulkanDevices(): List<String> {
            return VulkanDeviceDetector.detect().map { it.toString() }
        }

        actual suspend fun createSession(
            sourceSize: Pair<Int, Int>,
            modelName: String,
            times: Int,
            deviceIndex: Int,
        ): NcnnRunner {
            val modelParam = Res.readBytes("files/$modelName/flownet_opt.param")
            val modelBin = Res.readBytes("files/$modelName/flownet_opt.bin")
            val option = cLib.ncnn_option_create().apply {
                cLib.ncnn_option_set_num_threads(this, 1)
                cLib.ncnn_option_set_use_vulkan_compute(this, 1)
                cLib.ncnn_option_set_use_fp16_packed(this, 1)
                cLib.ncnn_option_set_use_fp16_storage(this, 1)
                cLib.ncnn_option_set_use_fp16_arithmetic(this, 1)
                cLib.ncnn_option_set_use_packing_layout(this, 1)
            }

            val pipelineCache = cLib.ncnn_pipelinecache_create(deviceIndex)
            cLib.ncnn_option_set_pipeline_cache(option, pipelineCache)

            val modelNetPtr = cLib.ncnn_net_create()
            cLib.ncnn_net_set_vulkan_device(modelNetPtr, deviceIndex)
            cLib.ncnn_net_set_option(modelNetPtr, option)
            val retParam = cLib.ncnn_net_load_param_memory(modelNetPtr, modelParam)
            require(retParam == 0L) { "load param failed: $retParam" }
            cLib.ncnn_net_load_model_memory(modelNetPtr, modelBin)

            val extractor = cLib.ncnn_extractor_create(modelNetPtr)
            cLib.ncnn_extractor_set_option(extractor, option)

            return NcnnRunner(
                sourceSize,
                modelNetPtr,
                option,
                extractor,
                pipelineCache,
                times
            )
        }
    }

    actual suspend fun upscale(inputPath: Path, outputPath: Path) {
        val image = loadImage(inputPath)
        logger.debug("upscale input: ${image.width}x${image.height}, target: ${targetWidth}x${targetHeight}")
        val padded = resizeWithPadding(image, targetWidth, targetHeight)
        val inputMat = createFloatMatFromImage(padded)
        val ex = cLib.ncnn_extractor_create(modelNetPtr)
        cLib.ncnn_extractor_set_option(ex, optionPtr)

        var outputMat: Pointer? = null
        try {
            check(cLib.ncnn_extractor_input(ex, "data", inputMat) == 0) { "upscale input failed." }
            val outRef = PointerByReference()
            val ret = cLib.ncnn_extractor_extract(ex, "output", outRef)
            check(ret == 0) { "extract failed: $ret" }
            outputMat = outRef.value
            check(outputMat != Pointer.NULL) { "output mat is null" }
            val outW = cLib.ncnn_mat_get_w(outputMat)
            val outH = cLib.ncnn_mat_get_h(outputMat)
            val outC = cLib.ncnn_mat_get_c(outputMat)
            logger.debug("upscale output: ${outW}x${outH}x${outC}")
            check(outC >= 3) { "expected at least 3 channels" }
            val fullImage = matToImage(outputMat, outW, outH)
            val cropped = cropToOriginal(fullImage, sourceSize.first * 2, sourceSize.second * 2)
            saveImage(cropped, outputPath)
        } finally {
            if (outputMat != null) {
                cLib.ncnn_mat_destroy(outputMat)
            }
            cLib.ncnn_mat_destroy(inputMat)
            cLib.ncnn_extractor_destroy(ex)
        }
    }

    actual suspend fun inferFrame(
        img0Path: Path,
        img1Path: Path,
        savePath: Path,
        timestep: Float
    ) {
        logger.info("run $savePath")
        val img0 = loadImage(img0Path)
        val img1 = loadImage(img1Path)
        logger.debug("inferFrame input: ${img0.width}x${img0.height}, ${img1.width}x${img1.height}, target: ${targetWidth}x${targetHeight}")
        val padded0 = resizeWithPadding(img0, targetWidth, targetHeight)
        val padded1 = resizeWithPadding(img1, targetWidth, targetHeight)
        val inputMat = create6ChannelMat(padded0, padded1, targetWidth, targetHeight)
        val tsMat = createScalarMat(timestep)
        val ex = cLib.ncnn_extractor_create(modelNetPtr)
        cLib.ncnn_extractor_set_option(ex, optionPtr)

        var outputMat: Pointer? = null
        try {
            check(cLib.ncnn_extractor_input(ex, "in0", inputMat) == 0) { "in0 input failed." }
            check(cLib.ncnn_extractor_input(ex, "in1", tsMat) == 0) { "in1 input failed." }
            val outRef = PointerByReference()
            val ret = cLib.ncnn_extractor_extract(ex, "out0", outRef)
            check(ret == 0) { "extract failed: $ret" }
            outputMat = outRef.value
            check(outputMat != Pointer.NULL) { "output mat is null" }
            val outW = cLib.ncnn_mat_get_w(outputMat)
            val outH = cLib.ncnn_mat_get_h(outputMat)
            val outC = cLib.ncnn_mat_get_c(outputMat)
            logger.debug("inferFrame output: ${outW}x${outH}x${outC}")
            check(outC >= 3) { "expected at least 3 channels" }
            val fullImage = matToImage(outputMat, outW, outH)
            val cropped = cropToOriginal(fullImage, sourceSize.first, sourceSize.second)
            saveImage(cropped, savePath)
        } finally {
            if (outputMat != null) {
                cLib.ncnn_mat_destroy(outputMat)
            }
            cLib.ncnn_mat_destroy(inputMat)
            cLib.ncnn_mat_destroy(tsMat)
            cLib.ncnn_extractor_destroy(ex)
        }
    }

    override fun close() {
        cLib.ncnn_extractor_destroy(extractorPtr)
        cLib.ncnn_net_destroy(modelNetPtr)
        cLib.ncnn_option_destroy(optionPtr)
        cLib.ncnn_pipelinecache_destroy(pipelineCachePtr)
    }

    private fun create6ChannelMat(
        img0: BufferedImage,
        img1: BufferedImage,
        w: Int,
        h: Int
    ): Pointer {
        val mat = cLib.ncnn_mat_create_3d_elem(
            w,
            h,
            6,
            elemsize,
            1,
            Pointer.NULL
        )

        check(mat != Pointer.NULL) {
            "failed to create 6-channel mat"
        }

        val dataPtr = cLib.ncnn_mat_get_data(mat)

        check(dataPtr != Pointer.NULL) {
            "data ptr is null"
        }

        val cstep = cLib.ncnn_mat_get_cstep(mat)
        val matElemsize = cLib.ncnn_mat_get_elemsize(mat)
        val channelByteOffset = cstep * matElemsize

        val r0 = dataPtr.share(0L)
        val g0 = dataPtr.share(1L * channelByteOffset)
        val b0 = dataPtr.share(2L * channelByteOffset)

        val r1 = dataPtr.share(3L * channelByteOffset)
        val g1 = dataPtr.share(4L * channelByteOffset)
        val b1 = dataPtr.share(5L * channelByteOffset)

        var index = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val rgb0 = img0.getRGB(x, y)
                val rgb1 = img1.getRGB(x, y)

                r0.setFloat(
                    index.toLong() * elemsize,
                    ((rgb0 shr 16) and 0xFF) / 255.0f
                )

                g0.setFloat(
                    index.toLong() * elemsize,
                    ((rgb0 shr 8) and 0xFF) / 255.0f
                )

                b0.setFloat(
                    index.toLong() * elemsize,
                    (rgb0 and 0xFF) / 255.0f
                )

                r1.setFloat(
                    index.toLong() * elemsize,
                    ((rgb1 shr 16) and 0xFF) / 255.0f
                )

                g1.setFloat(
                    index.toLong() * elemsize,
                    ((rgb1 shr 8) and 0xFF) / 255.0f
                )

                b1.setFloat(
                    index.toLong() * elemsize,
                    (rgb1 and 0xFF) / 255.0f
                )

                index++
            }
        }

        return mat
    }

    private fun cropToOriginal(
        image: BufferedImage,
        expectedW: Int,
        expectedH: Int
    ): BufferedImage {
        if (image.width == expectedW && image.height == expectedH) {
            return image
        }
        val offsetX = ((image.width - expectedW) / 2).coerceAtLeast(0)
        val offsetY = ((image.height - expectedH) / 2).coerceAtLeast(0)
        val cropW = minOf(expectedW, image.width - offsetX)
        val cropH = minOf(expectedH, image.height - offsetY)
        return image.getSubimage(offsetX, offsetY, cropW, cropH)
    }

    private fun createFloatMatFromImage(image: BufferedImage): Pointer {
        val w = image.width
        val h = image.height
        val mat = cLib.ncnn_mat_create_3d_elem(w, h, 3, elemsize, 1, Pointer.NULL)
        check(mat != Pointer.NULL) { "failed to create mat" }
        val dataPtr = cLib.ncnn_mat_get_data(mat)
        check(dataPtr != Pointer.NULL) { "data ptr is null" }

        val cstep = cLib.ncnn_mat_get_cstep(mat)
        val matElemsize = cLib.ncnn_mat_get_elemsize(mat)
        val planeSize = w * h
        val channelByteOffset = cstep * matElemsize

        val floatArray = FloatArray(planeSize * 3)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val rgb = image.getRGB(x, y)
                floatArray[idx] = ((rgb shr 16) and 0xFF) / 255.0f
                floatArray[idx + planeSize] = ((rgb shr 8) and 0xFF) / 255.0f
                floatArray[idx + 2 * planeSize] = (rgb and 0xFF) / 255.0f
            }
        }

        for (c in 0 until 3) {
            dataPtr.share(c * channelByteOffset).write(0, floatArray, c * planeSize, planeSize)
        }
        return mat
    }

    private fun createScalarMat(value: Float): Pointer {
        val mat = cLib.ncnn_mat_create_3d_elem(1, 1, 1, elemsize, 1, Pointer.NULL)
        cLib.ncnn_mat_fill_float(mat, value)
        return mat
    }

    // 核心修改：支持 elempack 和 fp16 的 matToImage
    private fun matToImage(mat: Pointer, width: Int, height: Int): BufferedImage {
        check(mat != Pointer.NULL) { "mat is null" }
        val c = cLib.ncnn_mat_get_c(mat)
        require(c >= 3) { "expected at least 3 channels, got $c" }

        val dataPtr = cLib.ncnn_mat_get_data(mat)
        check(dataPtr != Pointer.NULL) { "data pointer is null" }

        val elemsize = cLib.ncnn_mat_get_elemsize(mat).toInt()
        val elempack = cLib.ncnn_mat_get_elempack(mat).toInt()
        val cstep = cLib.ncnn_mat_get_cstep(mat)

        logger.debug("matToImage: w=$width h=$height c=$c elemsize=$elemsize elempack=$elempack cstep=$cstep")

        // 如果 elempack > 1，数据是交错存储的（像素连续）
        if (elempack > 1) {
            // 每个像素有 elempack 个通道，我们只需要前 3 个（假设输出为 RGB）
            val pixelSize = elempack * elemsize // 每个像素的字节数
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixelOffset = (y * width + x) * pixelSize
                    val r = readFloat(dataPtr.share(pixelOffset.toLong()), elemsize)
                    val g = readFloat(dataPtr.share(pixelOffset.toLong() + elemsize), elemsize)
                    val b = readFloat(dataPtr.share(pixelOffset.toLong() + 2 * elemsize), elemsize)
                    val ri = (r * 255).toInt().coerceIn(0, 255)
                    val gi = (g * 255).toInt().coerceIn(0, 255)
                    val bi = (b * 255).toInt().coerceIn(0, 255)
                    image.setRGB(x, y, (ri shl 16) or (gi shl 8) or bi)
                }
            }
            return image
        }

        // elempack == 1：平面存储
        // 通道间偏移 = cstep * elemsize 字节
        val channelByteOffset = cstep * elemsize
        val planeSize = width * height

        // 读取三个通道
        val rData = readChannel(dataPtr, 0, channelByteOffset, planeSize, elemsize)
        val gData = readChannel(dataPtr, 1, channelByteOffset, planeSize, elemsize)
        val bData = readChannel(dataPtr, 2, channelByteOffset, planeSize, elemsize)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        var idx = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (rData[idx] * 255).toInt().coerceIn(0, 255)
                val g = (gData[idx] * 255).toInt().coerceIn(0, 255)
                val b = (bData[idx] * 255).toInt().coerceIn(0, 255)
                image.setRGB(x, y, (r shl 16) or (g shl 8) or b)
                idx++
            }
        }
        return image
    }

    // 从指定指针读取一个 float（支持 float32 和 float16）
    private fun readFloat(ptr: Pointer, elemsize: Int): Float {
        return when (elemsize) {
            4 -> {
                ptr.getFloat(0)
            }

            2 -> {
                // 读取 half (unsigned short) 并转换
                val bits = ptr.getShort(0).toInt() and 0xFFFF
                halfToFloat(bits)
            }

            else -> {
                error("Unsupported elemsize: $elemsize")
            }
        }
    }

    // 读取一个完整通道的数据（平面存储）
    private fun readChannel(
        dataPtr: Pointer,
        channel: Int,
        channelByteOffset: Long,
        length: Int,
        elemsize: Int
    ): FloatArray {
        val startPtr = dataPtr.share(channel * channelByteOffset)
        val result = FloatArray(length)
        when (elemsize) {
            4 -> {
                startPtr.read(0, result, 0, length)
            }

            2 -> {
                val shorts = startPtr.getShortArray(0, length)
                for (i in 0 until length) {
                    result[i] = halfToFloat(shorts[i].toInt() and 0xFFFF)
                }
            }

            else -> {
                error("Unsupported elemsize: $elemsize")
            }
        }
        return result
    }

    // half (fp16) 转 float
    private fun halfToFloat(half: Int): Float {
        val sign = ((half shr 15) and 1) shl 31
        val exponent = ((half shr 10) and 0x1F)
        val mantissa = half and 0x3FF
        return when (exponent) {
            0 -> {
                // 非规格化数
                if (mantissa == 0) {
                    // 正负零
                    if (sign == 0) 0.0f else -0.0f
                } else {
                    // 计算非规格化数
                    val value = mantissa * (1.0f / (1 shl 24))
                    if (sign != 0) -value else value
                }
            }

            31 -> {
                if (mantissa == 0) {
                    if (sign == 0) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
                } else {
                    Float.NaN
                }
            }

            else -> {
                val bits = sign or ((exponent - 15 + 127) shl 23) or (mantissa shl 13)
                Float.fromBits(bits)
            }
        }
    }

    private suspend fun loadImage(path: Path): BufferedImage {
        val bytes = FileUtils.read(path)
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        } ?: throw IllegalArgumentException("Cannot load: $path")
    }

    private fun resizeWithPadding(
        image: BufferedImage,
        targetW: Int,
        targetH: Int
    ): BufferedImage {
        val w = image.width
        val h = image.height
        val ratio = minOf(targetW.toDouble() / w, targetH.toDouble() / h)
        val newW = (w * ratio).toInt()
        val newH = (h * ratio).toInt()
        val scaled = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)
        val padded = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
        val g = padded.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, targetW, targetH)
        val x = (targetW - newW) / 2
        val y = (targetH - newH) / 2
        g.drawImage(scaled, x, y, null)
        g.dispose()
        return padded
    }

    private fun saveImage(image: BufferedImage, path: Path) {
        FileUtils.getOutputStream(path) { sink ->
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            ImageIO.createImageOutputStream(sink.outputStream()).use {
                writer.setOutput(it)

                val writeParam = writer.defaultWriteParam
                writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
                writeParam.compressionQuality = 0.86f

                writer.write(null, IIOImage(image, null, null), writeParam)

            }
            writer.dispose()
        }
    }

    private fun ceilTo32(n: Int): Int = ((n + 31) / 32) * 32
}