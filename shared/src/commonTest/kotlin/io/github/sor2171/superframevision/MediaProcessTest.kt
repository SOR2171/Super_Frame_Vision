package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.service.MediaProcessor
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import kotlin.test.Test

class MediaProcessTest {
    val mediaProcessor = MediaProcessor(
        "D:\\Media\\Blender\\output\\meteor_Miku.mp4".toPath(),
        "D:\\Media\\Blender\\output\\tmp".toPath()
    )

    @Test
    fun detectFPS() {
        println(mediaProcessor.detectInputFrameRate())
    }

    @Test
    fun detectDimensions() {
        println(mediaProcessor.detectDimensions())
    }

    @Test
    fun inferFramesForVideo(): Unit = runBlocking {
        val originalFrameRate = mediaProcessor.detectInputFrameRate()!!
        check(mediaProcessor.extractFrames()) { "extractFrames" }
        check(mediaProcessor.renumberToOdd { this.originFrameDir }) { "renumberToOdd" }
        mediaProcessor.inferLeftFrames("rife-v4.26h", 8)
        mediaProcessor.encodeToMp4(originalFrameRate * 2) { this.inferredFrameDir }
    }
}