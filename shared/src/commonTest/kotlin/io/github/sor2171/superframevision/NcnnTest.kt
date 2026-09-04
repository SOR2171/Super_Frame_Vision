package io.github.sor2171.superframevision

import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.ptr.PointerByReference
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.NcnnLibrary
import io.github.sor2171.superframevision.core.utils.NcnnLoader
import io.github.sor2171.superframevision.core.utils.NcnnRunner
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.Test

class NcnnTest {
    val inputFolder = "D:/Code/Python/ModelTest/data/input_frames".toPath()
    val inputPNGList = FileUtils.list(inputFolder)
    val outputFolder = "D:/Code/Python/ModelTest/data/output_frames".toPath()

    val inputFrameList = List(inputPNGList.size) { i ->
        inputPNGList[i] to (inputPNGList.getOrNull(i + 1) ?: inputPNGList.last())
    }

    @Test
    fun listVulkanDevice() {
        NcnnRunner.listVulkanDevices()
        val output = StringBuilder()
        NcnnRunner.listVulkanDevices().forEach { output.append(it).append("\n") }
        println(output.toString())
    }

    @Test
    fun runRIFE() = runBlocking {
        NcnnRunner.createSession(
            1920 to 1080,
            "rife-v4.26h",
        ).use { it.processFrameInterpolation(inputFrameList, outputFolder) }
    }

    @Test
    fun runREAL() = runBlocking {
        NcnnRunner.createSession(
            1920 to 1080,
            "realesr-animevideov3-x2",
        ).use { it.processSuperResolution(inputPNGList, outputFolder) }
    }
}