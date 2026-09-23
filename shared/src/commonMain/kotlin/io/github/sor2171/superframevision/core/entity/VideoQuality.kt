package io.github.sor2171.superframevision.core.entity

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.video_quality_high
import superframevision.shared.generated.resources.video_quality_low
import superframevision.shared.generated.resources.video_quality_medium
import superframevision.shared.generated.resources.video_quality_very_high

@Serializable
enum class VideoQuality(
    override val label: StringResource,
    val crf: Int,
    val cq: Int,
    val globalQuality: Int,
    val videoToolboxQuality: Int,
    val cpuPreset: String,
    val nvencPreset: String,
    val amfQuality: String,
    val bitrateH264: String,
    val bitrateH265: String
) : LabelResolvable {
    VERY_HIGH(
        label = Res.string.video_quality_very_high,
        crf = 14,
        cq = 15,
        globalQuality = 15,
        videoToolboxQuality = 90,
        cpuPreset = "slow",
        nvencPreset = "p6",
        amfQuality = "quality",
        bitrateH264 = "25M",
        bitrateH265 = "16M"
    ),
    HIGH(
        label = Res.string.video_quality_high,
        crf = 18,
        cq = 19,
        globalQuality = 20,
        videoToolboxQuality = 75,
        cpuPreset = "medium",
        nvencPreset = "p5",
        amfQuality = "quality",
        bitrateH264 = "15M",
        bitrateH265 = "10M"
    ),
    MEDIUM(
        label = Res.string.video_quality_medium,
        crf = 23,
        cq = 24,
        globalQuality = 25,
        videoToolboxQuality = 60,
        cpuPreset = "medium",
        nvencPreset = "p4",
        amfQuality = "balanced",
        bitrateH264 = "8M",
        bitrateH265 = "5M"
    ),
    LOW(
        label = Res.string.video_quality_low,
        crf = 28,
        cq = 29,
        globalQuality = 30,
        videoToolboxQuality = 45,
        cpuPreset = "fast",
        nvencPreset = "p3",
        amfQuality = "speed",
        bitrateH264 = "4M",
        bitrateH265 = "2.5M"
    );

    fun buildEncodingOptions(codec: VideoCodec): Map<String, String> {
        val options = mutableMapOf<String, String>()
        options["-c:v"] = codec.codecName

        when (codec) {
            VideoCodec.LIBX264, VideoCodec.LIBX265 -> {
                options["-crf"] = crf.toString()
                options["-preset"] = cpuPreset
                options["-pix_fmt"] = "yuv420p"
            }
            VideoCodec.H264_NVENC, VideoCodec.HEVC_NVENC -> {
                options["-cq"] = cq.toString()
                options["-preset"] = nvencPreset
                options["-pix_fmt"] = "yuv420p"
            }
            VideoCodec.H264_QSV, VideoCodec.HEVC_QSV -> {
                options["-global_quality"] = globalQuality.toString()
                options["-preset"] = cpuPreset
                options["-pix_fmt"] = "nv12"
            }
            VideoCodec.H264_AMF, VideoCodec.HEVC_AMF -> {
                options["-rc"] = "cqp"
                options["-qp_i"] = cq.toString()
                options["-qp_p"] = cq.toString()
                options["-quality"] = amfQuality
                options["-pix_fmt"] = "yuv420p"
            }
            VideoCodec.H264_VIDEOTOOLBOX, VideoCodec.HEVC_VIDEOTOOLBOX -> {
                options["-q:v"] = videoToolboxQuality.toString()
                options["-pix_fmt"] = "yuv420p"
            }
            VideoCodec.H264_VAAPI, VideoCodec.HEVC_VAAPI -> {
                options["-qp"] = cq.toString()
            }
            VideoCodec.H264_MF, VideoCodec.HEVC_MF,
            VideoCodec.H264_D3D12VA, VideoCodec.HEVC_D3D12VA,
            VideoCodec.H264_VULKAN, VideoCodec.HEVC_VULKAN/*,
            VideoCodec.H264_MEDIACODEC, VideoCodec.HEVC_MEDIACODEC*/ -> {
                options["-b:v"] = if (codec.isH265) bitrateH265 else bitrateH264
                options["-pix_fmt"] = "yuv420p"
            }
        }
        return options
    }
}
