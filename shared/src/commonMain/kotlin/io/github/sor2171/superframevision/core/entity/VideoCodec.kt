package io.github.sor2171.superframevision.core.entity

import kotlinx.serialization.Serializable

@Serializable
enum class VideoCodec(
    val codecName: String,
    val displayName: String,
    val isH265: Boolean,
    val isHardware: Boolean
) {
    // === H.264 / AVC ===
    LIBX264("libx264", "H.264 (CPU)", isH265 = false, isHardware = false),
    H264_NVENC("h264_nvenc", "H.264 (NVIDIA NVENC)", isH265 = false, isHardware = true),
    H264_QSV("h264_qsv", "H.264 (Intel QSV)", isH265 = false, isHardware = true),
    H264_AMF("h264_amf", "H.264 (AMD AMF)", isH265 = false, isHardware = true),
    H264_MF("h264_mf", "H.264 (Windows MF)", isH265 = false, isHardware = true),
    H264_D3D12VA("h264_d3d12va", "H.264 (DirectX 12)", isH265 = false, isHardware = true),
    H264_VULKAN("h264_vulkan", "H.264 (Vulkan)", isH265 = false, isHardware = true),
    H264_VIDEOTOOLBOX("h264_videotoolbox", "H.264 (Apple VideoToolbox)", isH265 = false, isHardware = true),
    // H264_MEDIACODEC("h264_mediacodec", "H.264 (Android MediaCodec)", isH265 = false, isHardware = true),
    H264_VAAPI("h264_vaapi", "H.264 (Linux VAAPI)", isH265 = false, isHardware = true),

    // === H.265 / HEVC ===
    LIBX265("libx265", "H.265 (CPU)", isH265 = true, isHardware = false),
    HEVC_NVENC("hevc_nvenc", "H.265 (NVIDIA NVENC)", isH265 = true, isHardware = true),
    HEVC_QSV("hevc_qsv", "H.265 (Intel QSV)", isH265 = true, isHardware = true),
    HEVC_AMF("hevc_amf", "H.265 (AMD AMF)", isH265 = true, isHardware = true),
    HEVC_MF("hevc_mf", "H.265 (Windows MF)", isH265 = true, isHardware = true),
    HEVC_D3D12VA("hevc_d3d12va", "H.265 (DirectX 12)", isH265 = true, isHardware = true),
    HEVC_VULKAN("hevc_vulkan", "H.265 (Vulkan)", isH265 = true, isHardware = true),
    HEVC_VIDEOTOOLBOX("hevc_videotoolbox", "H.265 (Apple VideoToolbox)", isH265 = true, isHardware = true),
    // HEVC_MEDIACODEC("hevc_mediacodec", "H.265 (Android MediaCodec)", isH265 = true, isHardware = true),
    HEVC_VAAPI("hevc_vaapi", "H.265 (Linux VAAPI)", isH265 = true, isHardware = true);

    fun label(): String = displayName
}
