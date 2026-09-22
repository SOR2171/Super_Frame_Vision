@file:OptIn(ExperimentalAtomicApi::class)

package io.github.sor2171.superframevision.core.service

import io.github.sor2171.ffmpegkitkmp.FFmpegRunner
import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.isFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okio.Path
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class MediaProcessor private constructor(
    private val sourcePath: Path,
    private val outputPath: Path,
    tmpDir: Path
) : AutoCloseable {
    private val processOutputPath: Path = sourcePath.parent!! / "processed.mp4"
    val originFrameDir: Path = tmpDir / Const.ORIGIN_FRAME_DIR
    val upscaledFrameDir: Path = tmpDir / Const.UPSCALED_FRAME_DIR
    val inferredFrameDir: Path = tmpDir / Const.INFERRED_FRAME_DIR

    sealed interface NcnnTask {
        data class SuperResolution(
            val inputPath: Path,
            val outputPath: Path
        ) : NcnnTask

        data class FrameInterpolation(
            val img0Path: Path,
            val img1Path: Path,
            val savePath: Path,
            val timestep: Float = 0.5f
        ) : NcnnTask
    }

    val ncnnTaskList: MutableList<NcnnTask> = mutableListOf()
    private val taskIndex = AtomicInt(0)

    companion object {
        /** 默认编码参数（高质量、兼容性好） */
        val defaultEncodingOptions = mapOf(
            "-c:v" to "libx265",
            "-crf" to "14",
            "-pix_fmt" to "yuv420p",
            "-preset" to "medium"
        )

        /**
         * @param inputPath  输入视频路径
         * @param tmpDir     工作的缓存目录
         */
        suspend fun createSession(
            inputPath: Path,
            tmpDir: Path,
        ): MediaProcessor {
            val sourcePath = tmpDir / ("input_video." + inputPath.name.substringAfter("."))
            FileUtils.copy(inputPath, sourcePath)
            return MediaProcessor(
                sourcePath,
                inputPath.parent!! / inputPath.name.substringBefore("."),
                tmpDir
            )
        }
    }

    override fun close() {
        try {
            FileUtils.move(processOutputPath, outputPath)
            FileUtils.clearTmp()
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
        deviceIndex: Int,
        thread: Int = 4,
        originFrameDir: Path = this.originFrameDir,
        upscaledFrameDir: Path = this.upscaledFrameDir
    ) = coroutineScope {
        println("开始处理超分辨率，输出于$upscaledFrameDir")
        require(thread > 0) { "thread must be greater than 0" }

        FileUtils.createDirectories(upscaledFrameDir)

        val inputs =
            if (originFrameDir.isFile()) listOf(originFrameDir)
            else FileUtils.list(originFrameDir)

        ncnnTaskList.clear()
        inputs.forEach { path ->
            val savePath =
                if (originFrameDir.isFile())
                    upscaledFrameDir / "${path.name.substringBeforeLast(".")}_SR.jpg"
                else upscaledFrameDir / "${path.name.substringBeforeLast(".")}.jpg"
            ncnnTaskList.add(NcnnTask.SuperResolution(path, savePath))
        }
        taskIndex.store(0)

        val workerCount = minOf(thread, ncnnTaskList.size)

        repeat(workerCount) {
            launch(Dispatchers.Default) {
                val size = detectDimensions() ?: (1920 to 1080)

                NcnnRunner.createSession(
                    size,
                    model,
                    times = 2,
                    deviceIndex,
                ).use { runner ->
                    while (true) {
                        val index = taskIndex.fetchAndAdd(1)
                        if (index >= ncnnTaskList.size) break
                        val task = ncnnTaskList[index] as NcnnTask.SuperResolution
                        runner.upscale(task.inputPath, task.outputPath)
                    }
                }
            }
        }
    }

    suspend fun inferLeftFrames(
        model: Models,
        deviceIndex: Int,
        thread: Int = 4
    ) = coroutineScope {
        println("准备执行插帧，线程数：$thread")
        require(thread > 0) { "thread must be greater than 0" }

        val inputJpgList = FileUtils.list(inferredFrameDir)

        val inputFrameList = List(inputJpgList.size) { i ->
            inputJpgList[i] to (inputJpgList.getOrNull(i + 1) ?: inputJpgList.last())
        }

        FileUtils.createDirectories(inferredFrameDir)

        ncnnTaskList.clear()
        inputFrameList.forEach { (img0, img1) ->
            val idx = img0.name
                .substringBefore(".")
                .toInt()

            val savePath =
                inferredFrameDir / "${String.format("%06d", idx + 1)}.jpg"

            ncnnTaskList.add(NcnnTask.FrameInterpolation(img0, img1, savePath))
        }
        taskIndex.store(0)

        val workerCount = minOf(thread, ncnnTaskList.size)

        repeat(workerCount) {
            launch(Dispatchers.Default) {
                val size = detectDimensions() ?: (1920 to 1080)

                NcnnRunner.createSession(
                    size,
                    model,
                    times = 2,
                    deviceIndex,
                ).use { runner ->
                    while (true) {
                        val index = taskIndex.fetchAndAdd(1)
                        if (index >= ncnnTaskList.size) break
                        val task = ncnnTaskList[index] as NcnnTask.FrameInterpolation
                        runner.inferFrame(
                            task.img0Path,
                            task.img1Path,
                            task.savePath,
                            task.timestep
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
        require(frameRate.isFinite() && frameRate > 0.0) {
            "frameRate must be finite and greater than 0"
        }

        println("开始编码为 MP4，并复制原视频音轨：$processOutputPath")

        val finishDir = finishDirLambda(this)
        val optStr = options.entries.joinToString(" ") { "${it.key} ${it.value}" }

        val result = FFmpegRunner.execute(
            "-y",
            "-framerate $frameRate",
            "-start_number 1",
            "-i",
            quotePath(finishDir / "%06d.jpg"),
            "-i",
            quotePath(sourcePath),
            "-map 0:v:0",
            "-map 1:a?",
            optStr,
            "-c:a copy",
            quotePath(processOutputPath)
        )

        return !result.isNullOrBlank()
    }

    private fun quotePath(path: Path): String = "\"$path\""

    private fun clearDirectory(dir: Path) {
        FileUtils.list(dir).forEach { FileUtils.delete(it) }
    }
}