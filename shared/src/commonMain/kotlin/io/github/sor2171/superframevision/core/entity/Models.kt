package io.github.sor2171.superframevision.core.entity

enum class Models(
    val label: String,
    val is16: Boolean
) {
    RIFE4_26(label = "rife-v4.26h", is16 = true),
    REAL_A3_2(label = "realesr-animevideov3-x2", is16 = false)
}