package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.service.MediaProcessor
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
    fun extractFrames() {
        println(mediaProcessor.extractFrames())
    }
}