package io.github.sor2171.superframevision.core.entity

enum class Models(
    val size: Int,
    val label: String,
    val is16: Boolean
) {
    RIFE4_26(
        size = 1024,
        label = "rife-v4.26-1k",
        is16 = true
    ),
    REAL_A3_2(
        size = 1024,
        label = "realesr-animevideov3-x2",
        is16 = true
    )
}