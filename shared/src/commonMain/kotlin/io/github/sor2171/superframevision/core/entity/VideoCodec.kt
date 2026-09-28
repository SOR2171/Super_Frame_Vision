package io.github.sor2171.superframevision.core.entity

import kotlinx.serialization.Serializable

@Suppress("SpellCheckingInspection")
@Serializable
enum class VideoCodec(
    val codecName: String,
    val displayName: String,
    val isH265: Boolean,
    val isHardware: Boolean,
    val isAV1: Boolean = false
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
    HEVC_VAAPI("hevc_vaapi", "H.265 (Linux VAAPI)", isH265 = true, isHardware = true),

    // === AV1 ===
    LIBSVTAV1("libsvtav1", "AV1 (CPU SVT-AV1)", isH265 = false, isHardware = false, isAV1 = true),
    LIBAOM_AV1("libaom-av1", "AV1 (CPU libaom)", isH265 = false, isHardware = false, isAV1 = true),
    AV1_NVENC("av1_nvenc", "AV1 (NVIDIA NVENC)", isH265 = false, isHardware = true, isAV1 = true),
    AV1_QSV("av1_qsv", "AV1 (Intel QSV)", isH265 = false, isHardware = true, isAV1 = true),
    AV1_AMF("av1_amf", "AV1 (AMD AMF)", isH265 = false, isHardware = true, isAV1 = true),
    AV1_MF("av1_mf", "AV1 (Windows MF)", isH265 = false, isHardware = true, isAV1 = true),
    AV1_VULKAN("av1_vulkan", "AV1 (Vulkan)", isH265 = false, isHardware = true, isAV1 = true),
    // AV1_VIDEOTOOLBOX("av1_videotoolbox", "AV1 (Apple VideoToolbox)", isH265 = false, isHardware = true, isAV1 = true),
    // AV1_MEDIACODEC("av1_mediacodec", "AV1 (Android MediaCodec)", isH265 = false, isHardware = true, isAV1 = true),
    AV1_VAAPI("av1_vaapi", "AV1 (Linux VAAPI)", isH265 = false, isHardware = true, isAV1 = true);

    fun label(): String = displayName
}
