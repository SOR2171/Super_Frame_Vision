package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.entity.Models
import io.github.sor2171.superframevision.core.service.MediaProcessor
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import kotlin.test.Test

class MediaProcessTest {
    val mediaProcessor by lazy {
        runBlocking {
            MediaProcessor.createSession(
                "D:\\Media\\DaVinci\\星のカケラ_Miku_origin.mp4".toPath(),
                SettingsRepository.OverallSettings.default.workingDir.getPath()
            )
        }
    }

    @Test
    fun detectFPS() {
        println(mediaProcessor.detectInputFrameRate())
    }

    @Test
    fun detectDimensions() {
        println(mediaProcessor.detectDimensions())
    }

    @Test
    fun detectSceneTransitions() {
        val transitions = mediaProcessor.detectSceneTransitions(0.3)
        println("Detected scene transitions: $transitions")
    }

    @Test
    fun inferFramesForVideo(): Unit = runBlocking {
        val originalFrameRate = mediaProcessor.detectInputFrameRate()!!
        check(mediaProcessor.extractFrames()) { "extractFrames" }
        check(mediaProcessor.renumberToOdd(MediaProcessor::originFrameDir)) { "renumberToOdd" }
        mediaProcessor.inferLeftFrames(model = Models.RIFE4_26, deviceIndex = 1, thread = 2)
        mediaProcessor.encodeToMp4(originalFrameRate * 2) { this.inferredFrameDir }
    }

    @Test
    fun superResolutionForVideo(): Unit = runBlocking {
        check(mediaProcessor.extractFrames()) { "extractFrames" }
        mediaProcessor.processSuperResolution(model = Models.REAL_A3_2, deviceIndex = 1, thread = 2)
        mediaProcessor.encodeToMp4 { this.upscaledFrameDir }
    }
}