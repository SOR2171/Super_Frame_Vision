package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.NcnnRunner
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import kotlin.test.Test

class NcnnTest {
    val inputFolder = "D:/Code/Python/ModelTest/data/input_frames".toPath()
    val inputPNGList = FileUtils.list(inputFolder)
    val outputFolder = "D:/Code/Python/ModelTest/data/output_frames".toPath()

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
        ).use { it.inferFrame(inputPNGList[0], inputPNGList[1], outputFolder / "RIFE_ncnn.jpg") }
    }

    @Test
    fun runREAL() = runBlocking {
        NcnnRunner.createSession(
            1920 to 1080,
            "realesr-animevideov3-x2",
        ).use { it.upscale(inputPNGList[0], outputFolder / "REAL_ncnn.jpg") }
    }
}