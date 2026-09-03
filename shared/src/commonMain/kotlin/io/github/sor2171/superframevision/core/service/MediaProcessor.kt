package io.github.sor2171.superframevision.core.service

import io.github.sor2171.ffmpegkitkmp.FFmpegRunner
import io.github.sor2171.superframevision.core.utils.FileUtils
import okio.Path

/**
 * 1. 将输入视频的所有帧提取为 `%06d.jpg`
 * 2. 将所有帧重命名为 `(2n-1)` 形式的奇数编号
 * 3. 检查视频帧编号是否从 1 开始且连续
 * 4. 使用 concat 文件列表将帧序列压制为 MP4
 *
 * @param sourcePath  输入视频路径
 * @param workDir     存放帧的目录
 */
class MediaProcessor(
    private val sourcePath: Path,
    private val workDir: Path,
) {
    private val mp4OutputPath: Path
        get() = sourcePath.parent!! / "${sourcePath.name}_processed.mp4"

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
        FileUtils.createDirectories(workDir)
        clearDirectory(workDir)

        return !FFmpegRunner.execute(
            "-hwaccel auto",
            "-i",
            quotePath(sourcePath),
            "-f image2",
            "-fps_mode passthrough",
            "-q:v 2",
            quotePath(workDir / "%06d.jpg")
        ).isNullOrBlank()
    }

    fun renumberToOdd(): Boolean {
        val allJpgs = FileUtils.list(workDir).filter { it.name.endsWith(".jpg") }
        if (allJpgs.isEmpty()) return false

        // 按数字排序
        val sorted = allJpgs.mapNotNull {
            it.name.removeSuffix(".jpg").toIntOrNull()?.let { num -> num to it }
        }.sortedBy { it.first }

        val tempDir = workDir / "temp_rename"
        FileUtils.createDirectories(tempDir)

        var idx = 1
        for ((_, file) in sorted) {
            val newNum = 2 * idx - 1
            val newName = String.format("%06d.jpg", newNum)
            FileUtils.move(file, tempDir / newName)
            idx++
        }

        allJpgs.forEach { FileUtils.delete(it) }
        FileUtils.list(tempDir).forEach { file ->
            FileUtils.move(file, workDir / file.name)
        }
        FileUtils.delete(tempDir)
        return true
    }

    fun checkOddContinuity(): Boolean {
        val files = FileUtils.list(workDir).filter { it.name.endsWith(".jpg") }
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

    fun encodeToMp4(
        frameRate: Double, options: Map<String, String> = defaultEncodingOptions
    ): Boolean {
        // 生成文件列表（按文件名排序）
        val listFile = workDir / "filelist.txt"
        val files = FileUtils.list(workDir).filter { it.name.endsWith(".jpg") }
            .sortedBy { it.name.removeSuffix(".jpg").toIntOrNull() }

        // 构建列表内容（绝对路径）
        val listContent = files.joinToString("\n") { "file '$it'" }

        FileUtils.getOutputStream(listFile) { sink ->
            sink.writeUtf8(listContent)
            sink.flush()
        }

        // 构建 ffmpeg 命令
        val optStr = options.entries.joinToString(" ") { "${it.key} ${it.value}" }
        val success = FFmpegRunner.execute(
            "-framerate $frameRate",
            "-f concat",
            "-safe 0",
            "-i",
            quotePath(listFile),
            optStr,
            quotePath(mp4OutputPath)
        )
        // 清理临时列表文件
        FileUtils.delete(listFile)
        return !success.isNullOrBlank()
    }

    private fun quotePath(path: Path): String = "\"$path\""

    private fun clearDirectory(dir: Path) {
        FileUtils.list(dir).forEach { FileUtils.delete(it) }
    }

    companion object {
        /** 默认编码参数（高质量、兼容性好） */
        val defaultEncodingOptions = mapOf(
            "-c:v" to "libx264",
            "-crf" to "23",
            "-pix_fmt" to "yuv420p",
            "-preset" to "medium"
        )
    }
}