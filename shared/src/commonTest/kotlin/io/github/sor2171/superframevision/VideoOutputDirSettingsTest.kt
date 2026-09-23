package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.entity.VideoFormat
import io.github.sor2171.superframevision.core.utils.SettingsRepository.OverallSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoOutputDirSettingsTest {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testOverallSettingsDefaultVideoOutputDirIsNull() {
        val settings = OverallSettings.default
        assertNull(settings.videoOutputDir)
    }

    @Test
    fun testOverallSettingsSerializationWithCustomVideoOutputDir() {
        val customPath = "D:/Videos/Output"
        val settings = OverallSettings(videoOutputDir = customPath)
        val serialized = json.encodeToString(settings)

        val deserialized = json.decodeFromString<OverallSettings>(serialized)
        assertEquals(customPath, deserialized.videoOutputDir)
    }

    @Test
    fun testOverallSettingsBackwardCompatibilityWithoutField() {
        val jsonWithoutOutputDir = """
            {
                "themeColor": 1,
                "upscaleThread": 4,
                "inferThread": 16,
                "vulkanDevice": 0,
                "workingDir": "SystemTemp",
                "videoFormat": "MP4",
                "videoCodec": "LIBX265",
                "videoQuality": "HIGH"
            }
        """.trimIndent()

        val deserialized = json.decodeFromString<OverallSettings>(jsonWithoutOutputDir)
        assertNull(deserialized.videoOutputDir)
    }

    @Test
    fun testOutputPathLogicWhenVideoOutputDirIsNull() {
        val inputPath = "C:/Videos/source_video.mp4".toPath()
        val format = VideoFormat.MP4
        val videoOutputDir: okio.Path? = null

        val baseName = inputPath.name.substringBeforeLast(".")
        val outputFileName = if (inputPath.name == "$baseName.${format.extension}") {
            "${baseName}_processed.${format.extension}"
        } else {
            "$baseName.${format.extension}"
        }
        val outputDirectory = videoOutputDir ?: inputPath.parent!!
        val finalOutputPath = outputDirectory / outputFileName

        assertEquals("C:/Videos/source_video_processed.mp4".toPath(), finalOutputPath)
    }

    @Test
    fun testOutputPathLogicWhenVideoOutputDirIsCustom() {
        val inputPath = "C:/Videos/source_video.mp4".toPath()
        val format = VideoFormat.MP4
        val videoOutputDir: okio.Path? = "D:/CustomOutput".toPath()

        val baseName = inputPath.name.substringBeforeLast(".")
        val outputFileName = if (inputPath.name == "$baseName.${format.extension}") {
            "${baseName}_processed.${format.extension}"
        } else {
            "$baseName.${format.extension}"
        }
        val outputDirectory = videoOutputDir ?: inputPath.parent!!
        val finalOutputPath = outputDirectory / outputFileName

        assertEquals("D:/CustomOutput/source_video_processed.mp4".toPath(), finalOutputPath)
    }
}
