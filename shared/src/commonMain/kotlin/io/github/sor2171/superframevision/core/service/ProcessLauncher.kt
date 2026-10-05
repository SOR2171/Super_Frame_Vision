package io.github.sor2171.superframevision.core.service

import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.entity.ProcessType
import io.github.sor2171.superframevision.core.entity.QueueFile
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.isFile
import io.github.sor2171.superframevision.core.utils.getFileHeadTailHash
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import okio.Path.Companion.toPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.TimeMark

class ProcessLauncher(
    private val scope: CoroutineScope,
    private val queueFileList: SnapshotStateList<QueueFile>,
    private val getSettings: () -> SettingsRepository.OverallSettings,
    private val getProcessType: () -> ProcessType,
    private val onProcessingStateChange: (Boolean) -> Unit,
    private val onTaskProgressChange: (total: Int, completed: Int, startTime: TimeMark?, remainingTime: Duration?) -> Unit = { _, _, _, _ -> }
) {
    @Serializable
    private data class TaskInfo(
        val sourcePath: String,
        val processType: String,
        val sourceHash: String,
    )

    private val taskJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    var processJob: Job? = null
        private set

    var ncnnTaskTotal: Int = 0
        private set
    var ncnnTaskCompleted: Int = 0
        private set
    var queueStartTime: TimeMark? = null
        private set
    var remainingTime: Duration? = null
        private set

    private fun updateTaskProgress(total: Int, completed: Int, startTime: TimeMark? = null) {
        ncnnTaskTotal = total
        ncnnTaskCompleted = completed
        queueStartTime = startTime
        val remaining = if (startTime != null && completed > 0 && total > completed) {
            val elapsed = startTime.elapsedNow()
            elapsed * (total - completed) / completed
        } else if (total in 1..completed) {
            Duration.ZERO
        } else {
            null
        }
        remainingTime = remaining
        onTaskProgressChange(total, completed, startTime, remaining)
    }

    fun cancel() {
        println("已发送终止信号")
        processJob?.cancel()
        updateTaskProgress(0, 0, null)
    }

    fun start() {
        processJob = scope.launch(Dispatchers.Default) {
            onProcessingStateChange(true)
            updateTaskProgress(0, 0, null)
            try {
                while (queueFileList.isNotEmpty()) {
                    ensureActive()
                    updateTaskProgress(0, 0, null)

                    val queueFile = queueFileList.first()
                    queueFile.isProcessing.value = true

                    try {
                        val settings = getSettings()
                        val tmpDir = settings.workingDir.getPath()
                        val chosenProcessType = getProcessType()
                        val currentHash = getFileHeadTailHash(queueFile.path)
                        check(currentHash != null) { "Failed to get file hash for ${queueFile.path}" }
                        val taskInfoFile = tmpDir / "task_info.json"

                        val existingVideoInfo = FileUtils.list(tmpDir).firstOrNull {
                            it.name.startsWith("task_info.json") && it.isFile()
                        }

                        var isSameTask = false
                        if (existingVideoInfo != null) {
                            val content = FileUtils.read(existingVideoInfo)?.decodeToString()
                            val cached = content?.let {
                                runCatching { taskJson.decodeFromString<TaskInfo>(it) }.getOrNull()
                            }
                            if (cached != null &&
                                cached.sourcePath == queueFile.path.toString() &&
                                cached.processType == chosenProcessType.name &&
                                cached.sourceHash == currentHash
                            ) {
                                println("检测到缓存中的文件与当前任务一致，继续使用原数据")
                                isSameTask = true
                            } else {
                                println("检测到缓存中的文件与当前任务不一致，清空 tmp 目录")
                                FileUtils.clearTmp(tmpDir)
                            }
                        } else {
                            FileUtils.clearTmp(tmpDir)
                        }

                        if (!isSameTask) {
                            val taskInfo = TaskInfo(
                                sourcePath = queueFile.path.toString(),
                                processType = chosenProcessType.name,
                                sourceHash = currentHash
                            )
                            FileUtils.write(taskJson.encodeToString(taskInfo), taskInfoFile)
                        }

                        val encodingOptions =
                            settings.videoQuality.buildEncodingOptions(settings.videoCodec)

                        val customOutputDir =
                            settings.videoOutputDir?.takeIf { it.isNotBlank() }?.toPath()

                        MediaProcessor.createSession(
                            queueFile.path,
                            tmpDir,
                            settings.videoFormat,
                            customOutputDir,
                            onTaskProgress = ::updateTaskProgress
                        ).use { mediaProcessor ->
                            println("开始处理：$chosenProcessType ${queueFile.path}")

                            when (chosenProcessType) {
                                ProcessType.ImageSR -> {
                                    mediaProcessor.processSuperResolution(
                                        Models.REAL_A3_2,
                                        settings.vulkanDevice,
                                        1,
                                        queueFile.path,
                                        queueFile.path.parent!!
                                    )
                                }

                                ProcessType.VideoSR -> {
                                    check(mediaProcessor.extractFrames())
                                    { "Failed to extract frames" }
                                    mediaProcessor.processSuperResolution(
                                        Models.REAL_A3_2,
                                        settings.vulkanDevice,
                                        settings.upscaleThread
                                    )
                                    check(
                                        mediaProcessor.encodeToVideo(
                                            options = encodingOptions
                                        ) { this.upscaledFrameDir }
                                    )
                                    { "Failed to encode video" }
                                }

                                ProcessType.VideoFI -> {
                                    val originalFrameRate = mediaProcessor.detectInputFrameRate()
                                        ?: error("Failed to detect input frame rate")
                                    check(mediaProcessor.extractFrames())
                                    { "Failed to extract frames" }
                                    check(mediaProcessor.renumberToOdd(MediaProcessor::originFrameDir))
                                    { "Failed to renumber frames" }
                                    mediaProcessor.inferLeftFrames(
                                        Models.RIFE4_26,
                                        settings.vulkanDevice,
                                        settings.inferThread
                                    )
                                    check(
                                        mediaProcessor.encodeToVideo(
                                            originalFrameRate * 2,
                                            options = encodingOptions
                                        ) { this.inferredFrameDir }
                                    )
                                    { "Failed to encode video" }
                                }

                                ProcessType.VideoSRFI -> {
                                    val originalFrameRate = mediaProcessor.detectInputFrameRate()
                                        ?: error("Failed to detect input frame rate")
                                    check(mediaProcessor.extractFrames())
                                    { "Failed to extract frames" }
                                    check(mediaProcessor.renumberToOdd(MediaProcessor::originFrameDir))
                                    { "Failed to renumber frames" }
                                    mediaProcessor.inferLeftFrames(
                                        Models.RIFE4_26,
                                        settings.vulkanDevice,
                                        settings.inferThread
                                    )
                                    mediaProcessor.processSuperResolution(
                                        Models.REAL_A3_2,
                                        settings.vulkanDevice,
                                        settings.upscaleThread,
                                        originFrameDir = mediaProcessor.inferredFrameDir,
                                        upscaledFrameDir = mediaProcessor.upscaledFrameDir
                                    )
                                    check(
                                        mediaProcessor.encodeToVideo(
                                            originalFrameRate * 2,
                                            options = encodingOptions
                                        ) { this.upscaledFrameDir })
                                    { "Failed to encode video" }
                                }
                            }

                            mediaProcessor.isSuccessful = true
                        }
                        // finish process
                        queueFileList.removeFirstOrNull()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        println("Error processing file ${queueFile.path}:${e.message}")
                        e.printStackTrace()
                        // 发生异常时中断处理循环，保留当前文件在队列首位以便用户重试/恢复运行
                        break
                    } finally {
                        queueFile.isProcessing.value = false
                    }
                }
            } finally {
                updateTaskProgress(0, 0)
                onProcessingStateChange(false)
            }
        }
    }
}