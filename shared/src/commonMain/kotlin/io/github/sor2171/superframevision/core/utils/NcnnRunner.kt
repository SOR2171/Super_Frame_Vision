package io.github.sor2171.superframevision.core.utils

import okio.Path

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
expect class NcnnRunner : AutoCloseable {
    companion object {
        fun listVulkanDevices(): List<String>
        suspend fun createSession(
            sourceSize: Pair<Int, Int>,
            modelName: String,
            times: Int = 2,
            deviceIndex: Int = 1
        ): NcnnRunner
    }

    suspend fun upscale(inputPath: Path, outputPath: Path)
    suspend fun inferFrame(
        img0Path: Path,
        img1Path: Path,
        savePath: Path,
        timestep: Float = 0.5f
    )
}
