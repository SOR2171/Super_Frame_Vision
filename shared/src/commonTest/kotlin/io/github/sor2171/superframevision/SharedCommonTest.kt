package io.github.sor2171.superframevision

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
}