package io.github.sor2171.superframevision

import io.github.sor2171.ffmpegkitkmp.currentPlatform
import io.github.sor2171.superframevision.core.utils.FileUtils
import kotlin.test.Test

class SharedCommonTest {

    @Test
    fun fileTest() {
        println(FileUtils.installDir)
    }

    @Test
    fun showPlatform() {
        println(currentPlatform())
    }
}