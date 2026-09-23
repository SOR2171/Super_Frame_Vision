package io.github.sor2171.superframevision

import io.github.sor2171.superframevision.core.entity.VideoCodec
import io.github.sor2171.superframevision.core.entity.VideoFormat
import io.github.sor2171.superframevision.core.entity.VideoQuality
import io.github.sor2171.superframevision.core.utils.SettingsRepository.OverallSettings
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncodingSettingsTest {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testOverallSettingsSerialization() {
        val original = OverallSettings(
            videoFormat = VideoFormat.MKV,
            videoCodec = VideoCodec.HEVC_NVENC,
            videoQuality = VideoQuality.VERY_HIGH
        )
        val encoded = json.encodeToString(OverallSettings.serializer(), original)
        val decoded = json.decodeFromString(OverallSettings.serializer(), encoded)

        assertEquals(VideoFormat.MKV, decoded.videoFormat)
        assertEquals(VideoCodec.HEVC_NVENC, decoded.videoCodec)
        assertEquals(VideoQuality.VERY_HIGH, decoded.videoQuality)
    }

    @Test
    fun testBackwardCompatibility() {
        // Old JSON without videoFormat, videoCodec, videoQuality
        val oldJson = """
            {
                "themeColor": 0,
                "upscaleThread": 2,
                "inferThread": 8,
                "vulkanDevice": 1,
                "workingDir": "SystemTemp"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(OverallSettings.serializer(), oldJson)

        assertEquals(VideoFormat.MP4, decoded.videoFormat)
        assertEquals(VideoCodec.LIBX265, decoded.videoCodec)
        assertEquals(VideoQuality.HIGH, decoded.videoQuality)
    }

    @Test
    fun testEncodingOptionsAdaptation() {
        // x265 CPU: should use -crf and -preset
        val x265Opts = VideoQuality.HIGH.buildEncodingOptions(VideoCodec.LIBX265)
        assertEquals("libx265", x265Opts["-c:v"])
        assertEquals("18", x265Opts["-crf"])
        assertEquals("medium", x265Opts["-preset"])

        // NVENC: should use -cq and nvenc preset
        val nvencOpts = VideoQuality.VERY_HIGH.buildEncodingOptions(VideoCodec.HEVC_NVENC)
        assertEquals("hevc_nvenc", nvencOpts["-c:v"])
        assertEquals("15", nvencOpts["-cq"])
        assertEquals("p6", nvencOpts["-preset"])

        // QSV: should use -global_quality
        val qsvOpts = VideoQuality.MEDIUM.buildEncodingOptions(VideoCodec.H264_QSV)
        assertEquals("h264_qsv", qsvOpts["-c:v"])
        assertEquals("25", qsvOpts["-global_quality"])

        // All codecs should produce valid options
        VideoCodec.entries.forEach { codec ->
            val opts = VideoQuality.HIGH.buildEncodingOptions(codec)
            assertEquals(codec.codecName, opts["-c:v"])
            assertTrue(opts.isNotEmpty())
        }
    }
}
