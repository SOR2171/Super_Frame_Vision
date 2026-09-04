package io.github.sor2171.superframevision.core.utils

import okio.Path

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
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

    suspend fun processSuperResolution(inputs: List<Path>, outputDir: Path)
    suspend fun processFrameInterpolation(pairs: List<Pair<Path, Path>>, outputDir: Path)
}
