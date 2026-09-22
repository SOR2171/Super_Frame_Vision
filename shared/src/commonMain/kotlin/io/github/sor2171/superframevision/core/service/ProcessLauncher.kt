package io.github.sor2171.superframevision.core.service

import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.entity.ProcessType
import io.github.sor2171.superframevision.core.entity.QueueFile
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.isFile
import io.github.sor2171.superframevision.core.utils.isSameFile
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ProcessLauncher(
    private val scope: CoroutineScope,
    private val queueFileList: SnapshotStateList<QueueFile>,
    private val getSettings: () -> SettingsRepository.OverallSettings,
    private val getProcessType: () -> ProcessType,
    private val onProcessingStateChange: (Boolean) -> Unit
) {
    var processJob: Job? = null
        private set

    fun cancel() {
        processJob?.cancel()
    }

    fun start() {
        processJob = scope.launch(Dispatchers.Default) {
            onProcessingStateChange(true)
            try {
                while (queueFileList.isNotEmpty()) {
                    ensureActive()

                    val queueFile = queueFileList.first()
                    queueFile.isProcessing.value = true
                    var isCancelled = false

                    try {
                        val settings = getSettings()
                        val tmpDir = settings.workingDir.getPath()

                        val existingVideoInTmp = FileUtils.list(tmpDir).firstOrNull {
                            it.name.startsWith("input_video.") && it.isFile()
                        }

                        if (existingVideoInTmp != null) {
                            if (isSameFile(queueFile.path, existingVideoInTmp)) {
                                println("检测到缓存中的文件与当前任务一致，继续使用原数据")
                                val newExt = queueFile.path.name.substringAfter(".", "")
                                val expectedName = if (newExt.isEmpty()) "input_video" else "input_video.$newExt"
                                if (existingVideoInTmp.name != expectedName) {
                                    val targetPath = tmpDir / expectedName
                                    println("同步缓存文件名：${existingVideoInTmp.name} -> $expectedName")
                                    FileUtils.move(existingVideoInTmp, targetPath)
                                }
                            } else {
                                println("检测到缓存中的文件与当前任务不一致，清空 tmp 目录")
                                FileUtils.clearTmp(tmpDir)
                            }
                        } else {
                            FileUtils.clearTmp(tmpDir)
                        }

                        MediaProcessor.createSession(
                            queueFile.path,
                            tmpDir
                        ).use { mediaProcessor ->
                            val chosenProcessType = getProcessType()
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
                                    mediaProcessor.encodeToMp4 { this.upscaledFrameDir }
                                }

                                ProcessType.VideoFI -> {
                                    val originalFrameRate = mediaProcessor.detectInputFrameRate()
                                        ?: error("Failed to detect input frame rate")
                                    check(mediaProcessor.extractFrames())
                                    { "Failed to extract frames" }
                                    check(mediaProcessor.renumberToOdd { this.originFrameDir })
                                    { "Failed to renumber frames" }
                                    mediaProcessor.inferLeftFrames(
                                        Models.RIFE4_26,
                                        settings.vulkanDevice,
                                        settings.inferThread
                                    )
                                    mediaProcessor.encodeToMp4(originalFrameRate * 2) { this.inferredFrameDir }
                                }

                                ProcessType.VideoSRFI -> {
                                    val originalFrameRate = mediaProcessor.detectInputFrameRate()
                                        ?: error("Failed to detect input frame rate")
                                    check(mediaProcessor.extractFrames())
                                    { "Failed to extract frames" }
                                    mediaProcessor.processSuperResolution(
                                        Models.REAL_A3_2,
                                        settings.vulkanDevice,
                                        settings.upscaleThread
                                    )
                                    check(mediaProcessor.renumberToOdd { this.upscaledFrameDir })
                                    { "Failed to renumber frames" }
                                    mediaProcessor.inferLeftFrames(
                                        Models.RIFE4_26,
                                        settings.vulkanDevice,
                                        settings.inferThread
                                    )
                                    mediaProcessor.encodeToMp4(originalFrameRate * 2) { this.inferredFrameDir }
                                }
                            }

                            mediaProcessor.isSuccessful = true
                        }
                    } catch (e: CancellationException) {
                        isCancelled = true
                        throw e
                    } catch (e: Exception) {
                        println("Error processing file ${queueFile.path}:${e.message}")
                        e.printStackTrace()
                    } finally {
                        queueFile.isProcessing.value = false
                        if (!isCancelled) {
                            queueFileList.removeFirstOrNull()
                        }
                    }
                }
            } finally {
                onProcessingStateChange(false)
            }
        }
    }
}