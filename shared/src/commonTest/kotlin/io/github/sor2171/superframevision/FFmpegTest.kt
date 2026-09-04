package io.github.sor2171.superframevision

import io.github.sor2171.ffmpegkitkmp.FFmpegRunner
import kotlin.test.Test

class FFmpegTest {

    @Test
    fun FFmpegVersion() {
        println(FFmpegRunner.execute("-encoders"))
    }
}