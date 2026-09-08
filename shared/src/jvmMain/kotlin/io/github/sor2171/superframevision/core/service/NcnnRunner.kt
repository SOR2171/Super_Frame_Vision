package io.github.sor2171.superframevision.core.service

import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.VulkanDeviceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import org.slf4j.LoggerFactory
import superframevision.shared.generated.resources.Res
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "SameParameterValue")
actual class NcnnRunner(
    private val sourceSize: Pair<Int, Int>,
    private val modelNetPtr: Pointer,
    private val optionPtr: Pointer,
    private val pipelineCachePtr: Pointer,
    private val times: Int
) : AutoCloseable {
    private val elemsize = 4L

    actual companion object {
        private val logger = LoggerFactory.getLogger(NcnnRunner::class.java)
        private val cLib = NcnnLibrary.INSTANCE

        actual fun listVulkanDevices(): MutableList<String> {
            return VulkanDeviceDetector.detect().map{ it.name }.toMutableList()
        }

        actual suspend fun createSession(
            sourceSize: Pair<Int, Int>,
            model: Models,
            times: Int,
            deviceIndex: Int
        ): NcnnRunner {
            val modelParam = Res.readBytes("files/${model.label}/flownet_opt.param")
            val modelBin = Res.readBytes("files/${model.label}/flownet_opt.bin")

            val option = cLib.ncnn_option_create()
            check(option != Pointer.NULL) { "Failed to create ncnn option" }

            var pipelineCache: Pointer? = null
            var network: Pointer? = null

            try {
                cLib.ncnn_option_set_num_threads(option, 1)
                cLib.ncnn_option_set_use_vulkan_compute(option, 1)

                if (model.is16) {
                    cLib.ncnn_option_set_use_fp16_packed(option, 1)
                    cLib.ncnn_option_set_use_fp16_storage(option, 1)
                    cLib.ncnn_option_set_use_fp16_arithmetic(option, 1)
                    cLib.ncnn_option_set_use_packing_layout(option, 1)
                } else {
                    cLib.ncnn_option_set_use_fp16_packed(option, 0)
                    cLib.ncnn_option_set_use_fp16_storage(option, 0)
                    cLib.ncnn_option_set_use_fp16_arithmetic(option, 0)
                    cLib.ncnn_option_set_use_packing_layout(option, 0)
                }

                pipelineCache = cLib.ncnn_pipelinecache_create(deviceIndex)
                check(pipelineCache != Pointer.NULL) { "Failed to create ncnn pipeline cache" }

                cLib.ncnn_option_set_pipeline_cache(option, pipelineCache)

                network = cLib.ncnn_net_create()
                check(network != Pointer.NULL) { "Failed to create ncnn network" }

                cLib.ncnn_net_set_vulkan_device(network, deviceIndex)
                cLib.ncnn_net_set_option(network, option)

                val paramResult = cLib.ncnn_net_load_param_memory(network, modelParam)
                require(paramResult == 0L) { "Failed to load model param: $paramResult" }


                cLib.ncnn_net_load_model_memory(network, modelBin)

                val runner = NcnnRunner(
                    sourceSize = sourceSize,
                    modelNetPtr = network,
                    optionPtr = option,
                    pipelineCachePtr = pipelineCache,
                    times = times
                )

                network = null
                pipelineCache = null

                return runner
            } finally {
                if (network != null &&
                    network != Pointer.NULL
                ) {
                    cLib.ncnn_net_destroy(network)
                }

                if (pipelineCache != null &&
                    pipelineCache != Pointer.NULL
                ) {
                    cLib.ncnn_pipelinecache_clear(pipelineCache)
                    cLib.ncnn_pipelinecache_destroy(pipelineCache)
                }

                if (network != null) {
                    cLib.ncnn_option_destroy(option)
                }
            }
        }
    }

    private data class RifeTileOutput(
        val mat: Pointer?,
        val paddedWidth: Int,
        val paddedHeight: Int,
        val outputWidth: Int,
        val outputHeight: Int,
        val outputChannels: Int
    )

    private data class UpscaleTileOutput(
        val mat: Pointer,
        val inputWidth: Int,
        val inputHeight: Int,
        val outputWidth: Int,
        val outputHeight: Int,
        val outputChannels: Int
    )

    /**
     * RIFE 使用固定输入尺寸。
     *
     * 实际尺寸会取：
     *
     * min(floor32(tileMax), ceil32(imageSize))
     */
    private val rifeTileMaxWidth = 1024
    private val rifeTileMaxHeight = 1024
    private val rifeTilePadding = 64

    /**
     * Real-ESRGAN 使用最大 Tile 尺寸。
     *
     * 中间 Tile 通常为：
     *
     * padding + core + padding
     */
    private val upscaleTileMaxWidth = 1024
    private val upscaleTileMaxHeight = 1024
    private val upscaleTilePadding = 64

    actual suspend fun inferFrame(
        img0Path: Path,
        img1Path: Path,
        savePath: Path,
        timestep: Float
    ) {
        withContext(Dispatchers.IO) {
            logger.info("RIFE Tile inference: {}", savePath)

            val image0 = loadImage(img0Path)
            val image1 = loadImage(img1Path)

            require(image0.width == image1.width && image0.height == image1.height) {
                "RIFE input dimension mismatch: " +
                        "image0=${image0.width}x${image0.height}, " +
                        "image1=${image1.width}x${image1.height}"
            }

            require(image0.width > 0 && image0.height > 0) {
                "Invalid RIFE input size: ${image0.width}x${image0.height}"
            }

            val imageWidth = image0.width
            val imageHeight = image0.height

            val constTileWidth = minOf(
                floorTo32(rifeTileMaxWidth),
                ceilTo32(imageWidth)
            )

            val constTileHeight = minOf(
                floorTo32(rifeTileMaxHeight),
                ceilTo32(imageHeight)
            )

            require(constTileWidth > 0 && constTileHeight > 0) {
                "Invalid RIFE Tile size: ${constTileWidth}x${constTileHeight}"
            }

            var stepWidth = constTileWidth - rifeTilePadding * 2
            var stepHeight = constTileHeight - rifeTilePadding * 2

            if (stepWidth <= 0) {
                stepWidth = constTileWidth
            }

            if (stepHeight <= 0) {
                stepHeight = constTileHeight
            }

            val tileCountX = if (imageWidth <= constTileWidth) {
                1
            } else {
                ceilDiv(
                    imageWidth - rifeTilePadding * 2,
                    stepWidth
                ).coerceAtLeast(1)
            }

            val tileCountY = if (imageHeight <= constTileHeight) {
                1
            } else {
                ceilDiv(
                    imageHeight - rifeTilePadding * 2,
                    stepHeight
                ).coerceAtLeast(1)
            }

            val totalTileCountLong =
                tileCountX.toLong() * tileCountY.toLong()

            require(totalTileCountLong <= Int.MAX_VALUE) {
                "Too many RIFE Tiles: $totalTileCountLong"
            }

            val totalTileCount = totalTileCountLong.toInt()

            val outputPixels = IntArray(
                safePixelCount(imageWidth, imageHeight)
            )

            logger.info(
                "RIFE image={}x{}, constantTile={}x{}, grid={}x{}, total={}, pad={}",
                imageWidth,
                imageHeight,
                constTileWidth,
                constTileHeight,
                tileCountX,
                tileCountY,
                totalTileCount,
                rifeTilePadding
            )

            var currentTileIndex = 0
            var coveredY = 0

            for (tileIndexY in 0 until tileCountY) {
                val isLastY = tileIndexY == tileCountY - 1

                /*
                 * 最后一行直接贴住图像下边缘。
                 * Tile 输入尺寸始终保持不变。
                 */
                val tileY = if (isLastY || imageHeight <= constTileHeight) {
                    maxOf(0, imageHeight - constTileHeight)
                } else {
                    maxOf(
                        0,
                        coveredY - if (tileIndexY == 0) 0 else rifeTilePadding
                    )
                }

                var coveredX = 0

                for (tileIndexX in 0 until tileCountX) {
                    val isLastX = tileIndexX == tileCountX - 1

                    /*
                     * 最后一列直接贴住图像右边缘。
                     */
                    val tileX = if (isLastX || imageWidth <= constTileWidth) {
                        maxOf(0, imageWidth - constTileWidth)
                    } else {
                        maxOf(
                            0,
                            coveredX - if (tileIndexX == 0) 0 else rifeTilePadding
                        )
                    }

                    /*
                     * 只有当整张图比固定 Tile 小时，
                     * actualCrop 才会小于 constTile。
                     *
                     * 剩余区域由 createRife6ChannelInputMat 使用最边缘像素复制。
                     */
                    val actualCropWidth = minOf(
                        constTileWidth,
                        imageWidth - tileX
                    )

                    val actualCropHeight = minOf(
                        constTileHeight,
                        imageHeight - tileY
                    )

                    require(actualCropWidth > 0 && actualCropHeight > 0) {
                        "Invalid RIFE crop: x=$tileX y=$tileY " +
                                "size=${actualCropWidth}x${actualCropHeight}"
                    }

                    currentTileIndex++

                    logger.info(
                        "RIFE Tile {}/{}: input=({},{} {}x{}), tensor={}x{}",
                        currentTileIndex,
                        totalTileCount,
                        tileX,
                        tileY,
                        actualCropWidth,
                        actualCropHeight,
                        constTileWidth,
                        constTileHeight
                    )

                    val tileOutput = runRifeTile(
                        image0 = image0,
                        image1 = image1,
                        tileX = tileX,
                        tileY = tileY,
                        cropWidth = actualCropWidth,
                        cropHeight = actualCropHeight,
                        paddedWidth = constTileWidth,
                        paddedHeight = constTileHeight,
                        timestep = timestep
                    )

                    try {
                        require(tileOutput.outputChannels >= 3) {
                            "Unexpected RIFE output channels: " +
                                    "${tileOutput.outputChannels}"
                        }

                        /*
                         * 当前 Tile 应当负责的全图区域。
                         *
                         * 非最后一块丢弃右侧和下侧 padding。
                         * 最后一块一直负责到图像边缘。
                         */
                        val nextCoverX = if (isLastX) {
                            imageWidth
                        } else {
                            minOf(
                                imageWidth,
                                tileX + constTileWidth - rifeTilePadding
                            )
                        }

                        val nextCoverY = if (isLastY) {
                            imageHeight
                        } else {
                            minOf(
                                imageHeight,
                                tileY + constTileHeight - rifeTilePadding
                            )
                        }

                        val copyDestinationX = coveredX
                        val copyDestinationY = coveredY

                        val copyWidth = nextCoverX - coveredX
                        val copyHeight = nextCoverY - coveredY

                        val copySourceX = copyDestinationX - tileX
                        val copySourceY = copyDestinationY - tileY

                        if (copyWidth > 0 && copyHeight > 0) {
                            copyRifeTileToImage(
                                tileOutput = tileOutput,
                                destinationPixels = outputPixels,
                                destinationWidth = imageWidth,
                                destinationHeight = imageHeight,
                                destinationX = copyDestinationX,
                                destinationY = copyDestinationY,
                                sourceX = copySourceX,
                                sourceY = copySourceY,
                                copyWidth = copyWidth,
                                copyHeight = copyHeight
                            )
                        }

                        coveredX = nextCoverX
                    } finally {
                        cLib.ncnn_mat_destroy(tileOutput.mat)
                    }
                }

                coveredY = if (isLastY) {
                    imageHeight
                } else {
                    minOf(
                        imageHeight,
                        tileY + constTileHeight - rifeTilePadding
                    )
                }
            }

            check(coveredY == imageHeight) {
                "RIFE output was not completely covered: " +
                        "coveredY=$coveredY imageHeight=$imageHeight"
            }

            val outputImage = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)

            outputImage.setRGB(0, 0, imageWidth, imageHeight, outputPixels, 0, imageWidth)

            saveImage(outputImage, savePath)

            logger.info("RIFE output saved: {}", savePath)
        }
    }

    private fun runRifeTile(
        image0: BufferedImage,
        image1: BufferedImage,
        tileX: Int,
        tileY: Int,
        cropWidth: Int,
        cropHeight: Int,
        paddedWidth: Int,
        paddedHeight: Int,
        timestep: Float
    ): RifeTileOutput {
        var imageInputMat: Pointer? = null
        var timestepInputMat: Pointer? = null
        var extractor: Pointer? = null
        var extractedOutput: Pointer? = null
        var normalizedOutput: Pointer? = null

        try {
            imageInputMat = createRife6ChannelInputMat(
                image0 = image0,
                image1 = image1,
                tileX = tileX,
                tileY = tileY,
                cropWidth = cropWidth,
                cropHeight = cropHeight,
                paddedWidth = paddedWidth,
                paddedHeight = paddedHeight
            )

            timestepInputMat = createScalarMat(timestep)

            extractor = cLib.ncnn_extractor_create(modelNetPtr)

            check(extractor != Pointer.NULL) {
                "Failed to create RIFE extractor"
            }

            cLib.ncnn_extractor_set_option(extractor, optionPtr)

            check(
                cLib.ncnn_extractor_input(
                    extractor,
                    "in0",
                    imageInputMat
                ) == 0
            ) {
                "Failed to set RIFE input blob: in0"
            }

            check(
                cLib.ncnn_extractor_input(
                    extractor,
                    "in1",
                    timestepInputMat
                ) == 0
            ) {
                "Failed to set RIFE input blob: in1"
            }

            val outputReference = PointerByReference()

            val extractResult = cLib.ncnn_extractor_extract(
                extractor,
                "out0",
                outputReference
            )

            check(
                extractResult == 0 &&
                        outputReference.value != null &&
                        outputReference.value != Pointer.NULL
            ) {
                "Failed to extract RIFE output blob out0: $extractResult"
            }

            extractedOutput = outputReference.value

            val dims = cLib.ncnn_mat_get_dims(extractedOutput)

            normalizedOutput = when (dims) {
                3 -> {
                    val result = extractedOutput
                    extractedOutput = null
                    result
                }

                4 -> {
                    val depth = cLib.ncnn_mat_get_d(extractedOutput)

                    check(depth == 1) {
                        "Unsupported RIFE 4D output depth: $depth"
                    }

                    val width = cLib.ncnn_mat_get_w(extractedOutput)
                    val height = cLib.ncnn_mat_get_h(extractedOutput)
                    val channels = cLib.ncnn_mat_get_c(extractedOutput)

                    val reshaped = cLib.ncnn_mat_reshape_3d(
                        extractedOutput,
                        width,
                        height,
                        channels,
                        Pointer.NULL
                    )

                    check(reshaped != Pointer.NULL) {
                        "Failed to reshape RIFE output from 4D to 3D"
                    }

                    cLib.ncnn_mat_destroy(extractedOutput)
                    extractedOutput = null

                    reshaped
                }

                else -> {
                    error("Unsupported RIFE output dimensions: $dims")
                }
            }

            val result = RifeTileOutput(
                mat = normalizedOutput,
                paddedWidth = paddedWidth,
                paddedHeight = paddedHeight,
                outputWidth = cLib.ncnn_mat_get_w(normalizedOutput),
                outputHeight = cLib.ncnn_mat_get_h(normalizedOutput),
                outputChannels = cLib.ncnn_mat_get_c(normalizedOutput)
            )

            normalizedOutput = null
            return result
        } finally {
            if (normalizedOutput != null &&
                normalizedOutput != Pointer.NULL
            ) {
                cLib.ncnn_mat_destroy(normalizedOutput)
            }

            if (extractedOutput != null &&
                extractedOutput != Pointer.NULL
            ) {
                cLib.ncnn_mat_destroy(extractedOutput)
            }

            if (extractor != null &&
                extractor != Pointer.NULL
            ) {
                cLib.ncnn_extractor_destroy(extractor)
            }

            if (timestepInputMat != null &&
                timestepInputMat != Pointer.NULL
            ) {
                cLib.ncnn_mat_destroy(timestepInputMat)
            }

            if (imageInputMat != null &&
                imageInputMat != Pointer.NULL
            ) {
                cLib.ncnn_mat_destroy(imageInputMat)
            }
        }
    }

    private fun createRife6ChannelInputMat(
        image0: BufferedImage,
        image1: BufferedImage,
        tileX: Int,
        tileY: Int,
        cropWidth: Int,
        cropHeight: Int,
        paddedWidth: Int,
        paddedHeight: Int
    ): Pointer {
        require(cropWidth > 0 && cropHeight > 0)
        require(paddedWidth >= cropWidth)
        require(paddedHeight >= cropHeight)

        val mat = cLib.ncnn_mat_create_3d_elem(
            paddedWidth,
            paddedHeight,
            6,
            4L,
            1,
            Pointer.NULL
        )

        check(mat != Pointer.NULL) {
            "Failed to create RIFE 6-channel input Mat"
        }

        try {
            val channel0 = cLib.ncnn_mat_get_channel_data(mat, 0)
            val channel1 = cLib.ncnn_mat_get_channel_data(mat, 1)
            val channel2 = cLib.ncnn_mat_get_channel_data(mat, 2)
            val channel3 = cLib.ncnn_mat_get_channel_data(mat, 3)
            val channel4 = cLib.ncnn_mat_get_channel_data(mat, 4)
            val channel5 = cLib.ncnn_mat_get_channel_data(mat, 5)

            check(
                channel0 != Pointer.NULL &&
                        channel1 != Pointer.NULL &&
                        channel2 != Pointer.NULL &&
                        channel3 != Pointer.NULL &&
                        channel4 != Pointer.NULL &&
                        channel5 != Pointer.NULL
            ) {
                "Failed to access RIFE input channels"
            }

            val cropPixels0 = image0.getRGB(tileX, tileY, cropWidth, cropHeight, null, 0, cropWidth)
            val cropPixels1 = image1.getRGB(tileX, tileY, cropWidth, cropHeight, null, 0, cropWidth)

            /*
             * 这里必须使用 ncnn Mat 的实际 cstep。
             *
             * channel_data 已经考虑了 cstep，所以每个通道内部只需要按
             * y * paddedWidth + x 写入。
             */
            for (y in 0 until paddedHeight) {
                val sourceY = minOf(y, cropHeight - 1)

                for (x in 0 until paddedWidth) {
                    val sourceX = minOf(x, cropWidth - 1)

                    val sourceIndex = sourceY * cropWidth + sourceX
                    val destinationIndex = y * paddedWidth + x
                    val byteOffset = destinationIndex.toLong() * 4L

                    val rgb0 = cropPixels0[sourceIndex]
                    val rgb1 = cropPixels1[sourceIndex]

                    channel0.setFloat(byteOffset, ((rgb0 ushr 16) and 0xFF) / 255.0f)
                    channel1.setFloat(byteOffset, ((rgb0 ushr 8) and 0xFF) / 255.0f)
                    channel2.setFloat(byteOffset, (rgb0 and 0xFF) / 255.0f)
                    channel3.setFloat(byteOffset, ((rgb1 ushr 16) and 0xFF) / 255.0f)
                    channel4.setFloat(byteOffset, ((rgb1 ushr 8) and 0xFF) / 255.0f)
                    channel5.setFloat(byteOffset, (rgb1 and 0xFF) / 255.0f)
                }
            }

            return mat
        } catch (throwable: Throwable) {
            cLib.ncnn_mat_destroy(mat)
            throw throwable
        }
    }

    private fun copyRifeTileToImage(
        tileOutput: RifeTileOutput,
        destinationPixels: IntArray,
        destinationWidth: Int,
        destinationHeight: Int,
        destinationX: Int,
        destinationY: Int,
        sourceX: Int,
        sourceY: Int,
        copyWidth: Int,
        copyHeight: Int
    ) {
        require(tileOutput.outputChannels >= 3) {
            "RIFE output must have at least 3 channels"
        }

        require(destinationWidth > 0 && destinationHeight > 0)
        require(copyWidth > 0 && copyHeight > 0)

        require(destinationX >= 0 && destinationY >= 0)
        require(sourceX >= 0 && sourceY >= 0)

        require(destinationX + copyWidth <= destinationWidth) {
            "RIFE destination region exceeds output width"
        }

        require(destinationY + copyHeight <= destinationHeight) {
            "RIFE destination region exceeds output height"
        }

        require(sourceX + copyWidth <= tileOutput.outputWidth) {
            "RIFE source region exceeds Tile output width"
        }

        require(sourceY + copyHeight <= tileOutput.outputHeight) {
            "RIFE source region exceeds Tile output height"
        }

        val reader = MatPlanarReader(
            mat = tileOutput.mat,
            width = tileOutput.outputWidth,
            height = tileOutput.outputHeight
        )

        for (y in 0 until copyHeight) {
            val sourceRow = sourceY + y
            val destinationRow = destinationY + y

            for (x in 0 until copyWidth) {
                val sourceColumn = sourceX + x
                val destinationColumn = destinationX + x

                val sourceIndex = sourceRow * tileOutput.outputWidth + sourceColumn
                val destinationIndex = destinationRow * destinationWidth + destinationColumn

                val red = normalizedFloatToByte(reader.read(0, sourceIndex))
                val green = normalizedFloatToByte(reader.read(1, sourceIndex))
                val blue = normalizedFloatToByte(reader.read(2, sourceIndex))

                destinationPixels[destinationIndex] = (red shl 16) or (green shl 8) or blue
            }
        }
    }

    actual suspend fun upscale(
        inputPath: Path,
        outputPath: Path
    ) {
        withContext(Dispatchers.IO) {
            logger.info("Real-ESRGAN Tile inference: {}", outputPath)

            val inputImage = loadImage(inputPath)

            val imageWidth = inputImage.width
            val imageHeight = inputImage.height

            require(imageWidth > 0 && imageHeight > 0) {
                "Invalid input image size: ${imageWidth}x${imageHeight}"
            }

            require(
                upscaleTileMaxWidth > upscaleTilePadding * 2 &&
                        upscaleTileMaxHeight > upscaleTilePadding * 2
            ) {
                "Invalid upscale Tile configuration: " +
                        "tile=${upscaleTileMaxWidth}x${upscaleTileMaxHeight}, " +
                        "padding=$upscaleTilePadding"
            }

            val coreStepWidth = upscaleTileMaxWidth - upscaleTilePadding * 2
            val coreStepHeight = upscaleTileMaxHeight - upscaleTilePadding * 2

            val tileCountX = ceilDiv(imageWidth, coreStepWidth)
            val tileCountY = ceilDiv(imageHeight, coreStepHeight)

            val totalTileCountLong =
                tileCountX.toLong() * tileCountY.toLong()

            require(totalTileCountLong <= Int.MAX_VALUE) {
                "Too many upscale Tiles: $totalTileCountLong"
            }

            val totalTileCount = totalTileCountLong.toInt()

            logger.info(
                "Upscale input={}x{}, tileMaximum={}x{}, padding={}, " +
                        "coreStep={}x{}, grid={}x{}, total={}",
                imageWidth,
                imageHeight,
                upscaleTileMaxWidth,
                upscaleTileMaxHeight,
                upscaleTilePadding,
                coreStepWidth,
                coreStepHeight,
                tileCountX,
                tileCountY,
                totalTileCount
            )

            var scale = 0
            var outputWidth = 0
            var outputHeight = 0
            var outputPixels: IntArray? = null
            var currentTileIndex = 0

            var coreY = 0

            while (coreY < imageHeight) {
                val coreHeight = minOf(
                    coreStepHeight,
                    imageHeight - coreY
                )

                var coreX = 0

                while (coreX < imageWidth) {
                    val coreWidth = minOf(
                        coreStepWidth,
                        imageWidth - coreX
                    )

                    val tileX0 = maxOf(0, coreX - upscaleTilePadding)
                    val tileY0 = maxOf(0, coreY - upscaleTilePadding)

                    val tileX1 = minOf(imageWidth, coreX + coreWidth + upscaleTilePadding)
                    val tileY1 = minOf(imageHeight, coreY + coreHeight + upscaleTilePadding)

                    val tileWidth = tileX1 - tileX0
                    val tileHeight = tileY1 - tileY0

                    require(
                        tileWidth <= upscaleTileMaxWidth &&
                                tileHeight <= upscaleTileMaxHeight
                    ) {
                        "Internal upscale Tile size error: " +
                                "${tileWidth}x${tileHeight} exceeds " +
                                "${upscaleTileMaxWidth}x${upscaleTileMaxHeight}"
                    }

                    currentTileIndex++

                    logger.info(
                        "Upscale Tile {}/{}: input=({},{} {}x{}), " +
                                "core=({},{} {}x{})",
                        currentTileIndex,
                        totalTileCount,
                        tileX0,
                        tileY0,
                        tileWidth,
                        tileHeight,
                        coreX,
                        coreY,
                        coreWidth,
                        coreHeight
                    )

                    val tileOutput = runUpscaleTile(
                        image = inputImage,
                        tileX = tileX0,
                        tileY = tileY0,
                        tileWidth = tileWidth,
                        tileHeight = tileHeight
                    )

                    try {
                        require(tileOutput.outputChannels == 3) {
                            "Unexpected upscale output channels: " +
                                    "${tileOutput.outputChannels}, expected 3"
                        }

                        val currentScale = detectUpscaleScale(tileOutput)

                        if (scale == 0) {
                            scale = currentScale

                            outputWidth = safeMultiplyInt(
                                imageWidth,
                                scale,
                                "Upscale output width overflow"
                            )

                            outputHeight = safeMultiplyInt(
                                imageHeight,
                                scale,
                                "Upscale output height overflow"
                            )

                            outputPixels = IntArray(
                                safePixelCount(
                                    outputWidth,
                                    outputHeight
                                )
                            )

                            logger.info(
                                "Detected upscale model scale: x{}",
                                scale
                            )

                            logger.info(
                                "Upscale final output: {}x{}",
                                outputWidth,
                                outputHeight
                            )
                        } else {
                            require(currentScale == scale) {
                                "Inconsistent model scale: " +
                                        "first Tile=x$scale, current Tile=x$currentScale"
                            }
                        }

                        require(
                            tileOutput.outputWidth == tileWidth * scale &&
                                    tileOutput.outputHeight == tileHeight * scale
                        ) {
                            "Unexpected Tile output size: " +
                                    "got=${tileOutput.outputWidth}x" +
                                    "${tileOutput.outputHeight}, " +
                                    "expected=${tileWidth * scale}x" +
                                    "${tileHeight * scale}. " +
                                    "The model may be returning an aligned padded result."
                        }

                        copyUpscaleTileCoreToOutput(
                            tileOutput = tileOutput,
                            destinationPixels = checkNotNull(outputPixels),
                            destinationWidth = outputWidth,
                            destinationHeight = outputHeight,
                            scale = scale,
                            tileX = tileX0,
                            tileY = tileY0,
                            coreX = coreX,
                            coreY = coreY,
                            coreWidth = coreWidth,
                            coreHeight = coreHeight
                        )
                    } finally {
                        cLib.ncnn_mat_destroy(tileOutput.mat)
                    }

                    coreX += coreStepWidth
                }

                coreY += coreStepHeight
            }

            val finalPixels = checkNotNull(outputPixels) {
                "No upscale output was generated"
            }

            require(outputWidth > 0 && outputHeight > 0)

            val outputImage = BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB)

            outputImage.setRGB(0, 0, outputWidth, outputHeight, finalPixels, 0, outputWidth)

            saveImage(outputImage, outputPath)

            logger.info("Upscale result saved: {}", outputPath)
        }
    }

    private fun runUpscaleTile(
        image: BufferedImage,
        tileX: Int,
        tileY: Int,
        tileWidth: Int,
        tileHeight: Int
    ): UpscaleTileOutput {
        var inputMat: Pointer? = null
        var outputMat: Pointer? = null
        var extractor: Pointer? = null

        try {
            inputMat = createRgbFloatMatFromRegion(
                image = image,
                x = tileX,
                y = tileY,
                width = tileWidth,
                height = tileHeight
            )

            extractor = cLib.ncnn_extractor_create(modelNetPtr)

            check(extractor != Pointer.NULL) { "Failed to create upscale extractor" }

            cLib.ncnn_extractor_set_option(extractor, optionPtr)

            check(cLib.ncnn_extractor_input(extractor, "data", inputMat) == 0)
            { "Failed to set upscale input blob: data" }

            val outputReference = PointerByReference()

            val result = cLib.ncnn_extractor_extract(extractor, "output", outputReference)

            check(result == 0 && outputReference.value != null && outputReference.value != Pointer.NULL)
            { "Failed to extract upscale output blob output: $result" }

            outputMat = outputReference.value

            val tileOutput = UpscaleTileOutput(
                mat = outputMat,
                inputWidth = tileWidth,
                inputHeight = tileHeight,
                outputWidth = cLib.ncnn_mat_get_w(outputMat),
                outputHeight = cLib.ncnn_mat_get_h(outputMat),
                outputChannels = cLib.ncnn_mat_get_c(outputMat)
            )

            outputMat = null
            return tileOutput
        } finally {
            if (outputMat != null &&
                outputMat != Pointer.NULL
            ) {
                cLib.ncnn_mat_destroy(outputMat)
            }

            if (extractor != null &&
                extractor != Pointer.NULL
            ) {
                cLib.ncnn_extractor_destroy(extractor)
            }

            if (inputMat != null &&
                inputMat != Pointer.NULL
            ) {
                cLib.ncnn_mat_destroy(inputMat)
            }
        }
    }

    private fun createRgbFloatMatFromRegion(
        image: BufferedImage,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Pointer {
        require(x >= 0 && y >= 0)
        require(width > 0 && height > 0)
        require(x + width <= image.width)
        require(y + height <= image.height)

        val mat = cLib.ncnn_mat_create_3d_elem(width, height, 3, 4L, 1, Pointer.NULL)

        check(mat != Pointer.NULL) {
            "Failed to create RGB float Mat"
        }

        try {
            val redChannel = cLib.ncnn_mat_get_channel_data(mat, 0)
            val greenChannel = cLib.ncnn_mat_get_channel_data(mat, 1)
            val blueChannel = cLib.ncnn_mat_get_channel_data(mat, 2)

            check(
                redChannel != Pointer.NULL &&
                        greenChannel != Pointer.NULL &&
                        blueChannel != Pointer.NULL
            ) {
                "Failed to access RGB Mat channels"
            }

            val pixels = image.getRGB(x, y, width, height, null, 0, width)

            for (index in pixels.indices) {
                val rgb = pixels[index]
                val offset = index.toLong() * 4L

                redChannel.setFloat(offset, ((rgb ushr 16) and 0xFF) / 255.0f)
                greenChannel.setFloat(offset, ((rgb ushr 8) and 0xFF) / 255.0f)
                blueChannel.setFloat(offset, (rgb and 0xFF) / 255.0f)
            }

            return mat
        } catch (throwable: Throwable) {
            cLib.ncnn_mat_destroy(mat)
            throw throwable
        }
    }

    private fun detectUpscaleScale(
        output: UpscaleTileOutput
    ): Int {
        require(output.inputWidth > 0 && output.inputHeight > 0)
        require(output.outputWidth > 0 && output.outputHeight > 0)

        require(output.outputWidth % output.inputWidth == 0) {
            "Output width ${output.outputWidth} is not an integer " +
                    "multiple of input width ${output.inputWidth}"
        }

        require(output.outputHeight % output.inputHeight == 0) {
            "Output height ${output.outputHeight} is not an integer " +
                    "multiple of input height ${output.inputHeight}"
        }

        val scaleX = output.outputWidth / output.inputWidth
        val scaleY = output.outputHeight / output.inputHeight

        require(scaleX > 0 && scaleX == scaleY) {
            "Invalid model scale: scaleX=$scaleX scaleY=$scaleY"
        }

        return scaleX
    }

    private fun copyUpscaleTileCoreToOutput(
        tileOutput: UpscaleTileOutput,
        destinationPixels: IntArray,
        destinationWidth: Int,
        destinationHeight: Int,
        scale: Int,
        tileX: Int,
        tileY: Int,
        coreX: Int,
        coreY: Int,
        coreWidth: Int,
        coreHeight: Int
    ) {
        require(tileOutput.outputChannels == 3)
        require(scale > 0)

        require(coreX >= tileX && coreY >= tileY) {
            "Core region is outside Tile region"
        }

        val tileCoreX = coreX - tileX
        val tileCoreY = coreY - tileY

        val sourceX = tileCoreX * scale
        val sourceY = tileCoreY * scale

        val copyWidth = coreWidth * scale
        val copyHeight = coreHeight * scale

        val destinationX = coreX * scale
        val destinationY = coreY * scale

        require(
            sourceX >= 0 &&
                    sourceY >= 0 &&
                    copyWidth > 0 &&
                    copyHeight > 0
        ) {
            "Invalid Tile core source region"
        }

        require(
            sourceX + copyWidth <= tileOutput.outputWidth &&
                    sourceY + copyHeight <= tileOutput.outputHeight
        ) {
            "Tile core exceeds model output: " +
                    "source=($sourceX,$sourceY), " +
                    "size=${copyWidth}x$copyHeight, " +
                    "output=${tileOutput.outputWidth}x${tileOutput.outputHeight}"
        }

        require(
            destinationX + copyWidth <= destinationWidth &&
                    destinationY + copyHeight <= destinationHeight
        ) {
            "Tile core exceeds final output: " +
                    "destination=($destinationX,$destinationY), " +
                    "size=${copyWidth}x$copyHeight, " +
                    "output=${destinationWidth}x$destinationHeight"
        }

        val reader = MatPlanarReader(
            mat = tileOutput.mat,
            width = tileOutput.outputWidth,
            height = tileOutput.outputHeight
        )

        for (y in 0 until copyHeight) {
            val sourceRow = sourceY + y
            val destinationRow = destinationY + y

            for (x in 0 until copyWidth) {
                val sourceColumn = sourceX + x
                val destinationColumn = destinationX + x

                val sourceIndex = sourceRow * tileOutput.outputWidth + sourceColumn
                val destinationIndex = destinationRow * destinationWidth + destinationColumn

                val red = normalizedFloatToByte(reader.read(0, sourceIndex))
                val green = normalizedFloatToByte(reader.read(1, sourceIndex))
                val blue = normalizedFloatToByte(reader.read(2, sourceIndex))

                destinationPixels[destinationIndex] = (red shl 16) or (green shl 8) or blue
            }
        }
    }

    private inner class MatPlanarReader(
        mat: Pointer?,
        private val width: Int,
        private val height: Int
    ) {
        private val dataPointer: Pointer
        private val elementSize: Int
        private val elementPack: Int
        private val channelStep: Long
        private val scalarByteSize: Int
        private val channelByteStride: Long

        init {
            require(mat != Pointer.NULL) {
                "Mat is null"
            }

            require(width > 0 && height > 0)

            dataPointer = cLib.ncnn_mat_get_data(mat)

            check(dataPointer != Pointer.NULL) {
                "Mat data pointer is null"
            }

            elementSize = cLib.ncnn_mat_get_elemsize(mat).toInt()
            elementPack = cLib.ncnn_mat_get_elempack(mat).toInt()
            channelStep = cLib.ncnn_mat_get_cstep(mat)

            /*
             * 对 3 通道常规输出，预期为 elempack=1。
             *
             * 如果输出仍是 pack4 或 pack8，需要先调用
             * convert_packing 将其转换为 pack1。
             */
            require(elementPack == 1) {
                "Packed Mat output is not supported here: " +
                        "elempack=$elementPack, elemsize=$elementSize. " +
                        "Disable packing_layout for the model or unpack output to pack1."
            }

            scalarByteSize = elementSize

            require(scalarByteSize == 4 || scalarByteSize == 2) {
                "Unsupported Mat scalar size: $scalarByteSize"
            }

            channelByteStride =
                channelStep * scalarByteSize.toLong()
        }

        fun read(
            channel: Int,
            linearIndex: Int
        ): Float {
            require(channel >= 0)
            require(linearIndex >= 0)
            require(linearIndex < width * height)

            val offset = channel.toLong() * channelByteStride +
                    linearIndex.toLong() * scalarByteSize.toLong()

            return when (scalarByteSize) {
                4 -> dataPointer.getFloat(offset)

                2 -> {
                    val bits = dataPointer.getShort(offset).toInt() and 0xFFFF
                    halfToFloat(bits)
                }

                else -> error("Unsupported Mat scalar size: $scalarByteSize")
            }
        }
    }

    private fun normalizedFloatToByte(value: Float): Int {
        if (value.isNaN()) {
            return 0
        }

        if (value <= 0.0f) {
            return 0
        }

        if (value >= 1.0f) {
            return 255
        }

        return (value * 255.0f + 0.5f)
            .toInt()
            .coerceIn(0, 255)
    }

    private fun floorTo32(value: Int): Int {
        if (value <= 0) {
            return 0
        }

        return value / 32 * 32
    }

    private fun ceilTo32(value: Int): Int {
        if (value <= 0) {
            return 0
        }

        require(value <= Int.MAX_VALUE - 31) {
            "ceilTo32 overflow: $value"
        }

        return (value + 31) / 32 * 32
    }

    private fun ceilDiv(
        value: Int,
        divisor: Int
    ): Int {
        require(divisor > 0)

        if (value <= 0) {
            return 0
        }

        return ((value.toLong() + divisor - 1L) / divisor)
            .also {
                require(it <= Int.MAX_VALUE) {
                    "ceilDiv result overflow: $it"
                }
            }
            .toInt()
    }

    private fun safeMultiplyInt(
        first: Int,
        second: Int,
        message: String
    ): Int {
        val result = first.toLong() * second.toLong()

        require(result in 0..Int.MAX_VALUE.toLong()) {
            message
        }

        return result.toInt()
    }

    private fun safePixelCount(
        width: Int,
        height: Int
    ): Int {
        require(width > 0 && height > 0)

        val count = width.toLong() * height.toLong()

        require(count <= Int.MAX_VALUE) {
            "Image is too large for JVM IntArray: " +
                    "${width}x$height, pixels=$count"
        }

        return count.toInt()
    }

    private fun halfToFloat(half: Int): Float {
        val sign = ((half ushr 15) and 1) shl 31
        val exponent = (half ushr 10) and 0x1F
        val mantissa = half and 0x3FF

        return when (exponent) {
            0 -> {
                if (mantissa == 0) {
                    Float.fromBits(sign)
                } else {
                    var normalizedMantissa = mantissa
                    var normalizedExponent = -14

                    while ((normalizedMantissa and 0x400) == 0) {
                        normalizedMantissa = normalizedMantissa shl 1
                        normalizedExponent--
                    }

                    normalizedMantissa =
                        normalizedMantissa and 0x3FF

                    val floatExponent =
                        normalizedExponent + 127

                    Float.fromBits(
                        sign or
                                (floatExponent shl 23) or
                                (normalizedMantissa shl 13)
                    )
                }
            }

            31 -> {
                if (mantissa == 0) {
                    Float.fromBits(sign or 0x7F800000)
                } else {
                    Float.fromBits(sign or 0x7F800000 or (mantissa shl 13))
                }
            }

            else -> {
                val floatExponent = exponent - 15 + 127
                Float.fromBits(sign or (floatExponent shl 23) or (mantissa shl 13))
            }
        }
    }

    private fun createScalarMat(value: Float): Pointer {
        val mat = cLib.ncnn_mat_create_3d_elem(1, 1, 1, elemsize, 1, Pointer.NULL)
        cLib.ncnn_mat_fill_float(mat, value)
        return mat
    }

    private suspend fun loadImage(path: Path): BufferedImage {
        val bytes = FileUtils.read(path)
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        } ?: throw IllegalArgumentException("Cannot load: $path")
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

    actual override fun close() {
        if (modelNetPtr != Pointer.NULL) {
            cLib.ncnn_net_destroy(modelNetPtr)
        }
        if (pipelineCachePtr != Pointer.NULL) {
            cLib.ncnn_pipelinecache_clear(pipelineCachePtr)
            cLib.ncnn_pipelinecache_destroy(pipelineCachePtr)
        }
        if (optionPtr != Pointer.NULL) {
            cLib.ncnn_option_destroy(optionPtr)
        }
    }
}