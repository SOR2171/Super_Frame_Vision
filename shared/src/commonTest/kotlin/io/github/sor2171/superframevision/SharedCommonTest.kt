package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.entity.currentPlatform
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.FileUtils
import kotlin.test.Test

class SharedCommonTest {

    @Test
    fun fileTest() {
        println(FileUtils.installDir)
    }

    @Test
    fun platformTest() {
        println(currentPlatform())
    }

    @Test
    fun constantTest() {
        Const.javaClass.declaredFields.forEach { field ->
            try {
                field.isAccessible = true
                println("${field.name} = ${field.get(Const)}")
            } catch (e: Exception) {
                println("${field.name} = [Cannot access: ${e.message}]")
            }
        }
    }
}