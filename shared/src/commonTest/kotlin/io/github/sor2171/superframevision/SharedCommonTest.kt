package io.github.sor2171.superframevision

import io.github.sor2171.ffmpegkitkmp.currentPlatform
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.createFileUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun fileTest() {
        val fileUtils = createFileUtils()
        println(fileUtils.installDir)
    }

    @Test
    fun platformInFFmpeg() {
        println(currentPlatform())
        Thread.sleep(5 * 1000)
    }
}