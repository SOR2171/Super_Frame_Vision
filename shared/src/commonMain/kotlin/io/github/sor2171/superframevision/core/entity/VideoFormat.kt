package io.github.sor2171.superframevision.core.entity

import kotlinx.serialization.Serializable

@Serializable
enum class VideoFormat(
    val extension: String,
    val displayName: String
) {
    MP4("mp4", "MP4 (.mp4)"),
    MKV("mkv", "MKV (.mkv)"),
    MOV("mov", "MOV (.mov)");

    fun label(): String = displayName
}
