package io.github.sor2171.superframevision.core.service

import io.github.sor2171.ffmpegkitkmp.FFmpegRunner
import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.isFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import okio.Path

/**
 * 1. 将输入视频的所有帧提取为 `%06d.jpg`
 * 2. 将所有帧重命名为 `(2n-1)` 形式的奇数编号
 * 3. 检查视频帧编号是否从 1 开始且连续
 * 4. 使用 concat 文件列表将帧序列压制为 MP4
 *
 * @param sourcePath  输入视频路径
 * @param tmpDir     工作的缓存目录
 */
class MediaProcessor(
    private val sourcePath: Path,
    private val tmpDir: Path,
) : AutoCloseable {
    private val videoName = sourcePath.name.substringBefore(".")
    private val mp4OutputPath: Path
        get() = sourcePath.parent!! / "${videoName}_processed.mp4"

    val originFrameDir = tmpDir / Const.ORIGIN_FRAME_DIR
    val upscaledFrameDir = tmpDir / Const.UPSCALED_FRAME_DIR
    val inferredFrameDir = tmpDir / Const.INFERRED_FRAME_DIR

    override fun close() {
        try {
            FileUtils.list(originFrameDir).forEach { file ->
                FileUtils.delete(file)
            }
            FileUtils.list(upscaledFrameDir).forEach { file ->
                FileUtils.delete(file)
            }
            FileUtils.list(inferredFrameDir).forEach { file ->
                FileUtils.delete(file)
            }
        } catch (e: Exception) {
            println("Error occurred while cleaning directories: ${e.message}")
        }
    }

    fun detectInputFrameRate(): Double? {
        try {
            val result = FFmpegRunner.ffprobe(
                "-v error",
                "-select_streams",
                "v:0",
                "-show_entries",
                "stream=r_frame_rate",
                "-of",
                "default=noprint_wrappers=1",
                quotePath(sourcePath)
            )

            if (result.isNullOrBlank())
                throw Exception("ffprobe returned empty result")

            val frameRateStr = result.substringAfter("=").trim()
            val parts = frameRateStr.split('/')
            val fps = if (parts.size == 2) {
                parts[0].toDouble() / parts[1].toDouble()
            } else {
                frameRateStr.toDouble()
            }
            return fps
        } catch (e: Exception) {
            println("cannot get frame rate: ${e.message}")
            return null
        }
    }

    fun detectDimensions(): Pair<Int, Int>? {
        try {
            val result = FFmpegRunner.ffprobe(
                "-v error",
                "-select_streams",
                "v:0",
                "-show_entries",
                "stream=width,height",
                "-of",
                "default=noprint_wrappers=1",
                quotePath(sourcePath)
            )
            if (result.isNullOrBlank()) return null
            val lines = result.lines().filter { it.isNotBlank() }
            var width: Int? = null
            var height: Int? = null
            for (line in lines) {
                when {
                    line.startsWith("width=") -> width =
                        line.substringAfter("=").trim().toIntOrNull()

                    line.startsWith("height=") -> height =
                        line.substringAfter("=").trim().toIntOrNull()
                }
            }
            return if (width != null && height != null) width to height else null
        } catch (e: Exception) {
            println("cannot get dimensions: ${e.message}")
            return null
        }
    }

    fun extractFrames(): Boolean {
        println("提取帧序列至 $originFrameDir")
        FileUtils.createDirectories(originFrameDir)
        clearDirectory(originFrameDir)

        return !FFmpegRunner.execute(
            "-hwaccel auto",
            "-i",
            quotePath(sourcePath),
            "-f image2",
            "-fps_mode passthrough",
            "-q:v 2",
            quotePath(originFrameDir / "%06d.jpg"),
            "-y"
        ).isNullOrBlank()
    }

    fun renumberToOdd(sourcePath: MediaProcessor.() -> Path): Boolean {
        println("重新编号奇数帧至 $inferredFrameDir")
        val sourceDir = sourcePath()
        FileUtils.createDirectories(inferredFrameDir)
        val allJpgs = FileUtils.list(sourceDir).filter { it.name.endsWith(".jpg") }
        if (allJpgs.isEmpty()) return false

        // 按数字排序
        val sorted = allJpgs.mapNotNull {
            it.name.removeSuffix(".jpg").toIntOrNull()?.let { num -> num to it }
        }.sortedBy { it.first }

        var idx = 1
        for ((_, file) in sorted) {
            val newNum = 2 * idx - 1
            val newName = String.format("%06d.jpg", newNum)
            FileUtils.move(file, inferredFrameDir / newName)
            idx++
        }

        return checkOddContinuity()
    }

    fun checkOddContinuity(): Boolean {
        println("检查奇数帧连续性")
        val files = FileUtils.list(inferredFrameDir).filter { it.name.endsWith(".jpg") }
        if (files.isEmpty()) return false

        val nums = files.mapNotNull {
            it.name.removeSuffix(".jpg").toIntOrNull()
        }.sorted()

        for (i in nums.indices) {
            val expected = 2 * (i + 1) - 1
            if (nums[i] != expected) {
                println("Frame sequence broken: expected $expected, got ${nums[i]}")
                return false
            }
        }
        return true
    }

    suspend fun processSuperResolution(
        model: Models,
        thread: Int = 4,
        originFrameDir: Path = this.originFrameDir,
        upscaledFrameDir: Path = this.upscaledFrameDir
    ) = coroutineScope {
        println("开始处理超分辨率，输出于$upscaledFrameDir")
        launch(Dispatchers.Default) {
            require(thread > 0) { "thread must be greater than 0" }

            FileUtils.createDirectories(upscaledFrameDir)

            val inputs =
                if (originFrameDir.isFile()) listOf(originFrameDir)
                else FileUtils.list(originFrameDir)
            val workerCount = minOf(thread, inputs.size)

            repeat(workerCount) { workerId ->
                val start = inputs.size * workerId / workerCount
                val end = inputs.size * (workerId + 1) / workerCount

                val paths = inputs.subList(start, end)
                val size = detectDimensions() ?: (1920 to 1080)

                NcnnRunner.createSession(
                    size,
                    model.toLarger(size),
                ).use { runner ->
                    paths.forEach { path ->
                        val savePath =
                            if (originFrameDir.isFile())
                                upscaledFrameDir / "${path.name.substringBeforeLast(".")}_SR.jpg"
                            else upscaledFrameDir / "${path.name.substringBeforeLast(".")}.jpg"
                        runner.upscale(path, savePath)
                    }
                }
            }
        }
    }

    suspend fun inferLeftFrames(
        model: Models,
        thread: Int = 4
    ) = coroutineScope {
        println("准备执行插帧，线程数：$thread")
        require(thread > 0) { "thread must be greater than 0" }

        val inputJpgList = FileUtils.list(inferredFrameDir)

        val inputFrameList = List(inputJpgList.size) { i ->
            inputJpgList[i] to (inputJpgList.getOrNull(i + 1) ?: inputJpgList.last())
        }

        FileUtils.createDirectories(inferredFrameDir)

        val workerCount = minOf(thread, inputFrameList.size)

        repeat(workerCount) { workerId ->
            launch(Dispatchers.Default) {
                val start = inputFrameList.size * workerId / workerCount
                val end = inputFrameList.size * (workerId + 1) / workerCount

                val pairs = inputFrameList.subList(start, end)
                val size = detectDimensions() ?: (1920 to 1080)

                NcnnRunner.createSession(
                    size,
                    model.toLarger(size),
                ).use { runner ->
                    pairs.forEach { (img0, img1) ->
                        val idx = img0.name
                            .substringBefore(".")
                            .toInt()

                        val savePath =
                            inferredFrameDir / "${String.format("%06d", idx + 1)}.jpg"

                        runner.inferFrame(
                            img0,
                            img1,
                            savePath,
                            0.5f
                        )
                    }
                }
            }
        }
    }

    fun encodeToMp4(
        frameRate: Double = detectInputFrameRate() ?: 30.0,
        options: Map<String, String> = defaultEncodingOptions,
        finishDirLambda: MediaProcessor.() -> Path
    ): Boolean {
        println("开始编码为 MP4：$mp4OutputPath")
        val finishDir = finishDirLambda(this)
        val optStr = options.entries.joinToString(" ") { "${it.key} ${it.value}" }
        val success = FFmpegRunner.execute(
            "-framerate $frameRate",
            "-i",
            quotePath(finishDir / "%06d.jpg"),
            optStr,
            quotePath(mp4OutputPath),
            "-y"
        )
        return !success.isNullOrBlank()
    }

    private fun quotePath(path: Path): String = "\"$path\""

    private fun clearDirectory(dir: Path) {
        FileUtils.list(dir).forEach { FileUtils.delete(it) }
    }

    companion object {
        /** 默认编码参数（高质量、兼容性好） */
        val defaultEncodingOptions = mapOf(
            "-c:v" to "libx265",
            "-crf" to "14",
            "-pix_fmt" to "yuv420p",
            "-preset" to "medium"
        )
    }
}