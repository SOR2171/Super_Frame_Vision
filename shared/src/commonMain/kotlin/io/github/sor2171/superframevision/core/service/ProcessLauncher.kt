package io.github.sor2171.superframevision.core.service

import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.entity.ProcessType
import io.github.sor2171.superframevision.core.entity.QueueFile
import io.github.sor2171.superframevision.core.utils.FileUtils
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

                    try {
                        MediaProcessor.createSession(
                            queueFile.path, FileUtils.basicTmpDir
                        ).use { mediaProcessor ->
                            val chosenProcessType = getProcessType()
                            val settings = getSettings()
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
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        println("Error processing file ${queueFile.path}:${e.message}")
                        e.printStackTrace()
                    } finally {
                        queueFile.isProcessing.value = false
                        queueFileList.removeFirstOrNull()
                    }
                }
            } finally {
                onProcessingStateChange(false)
            }
        }
    }
}