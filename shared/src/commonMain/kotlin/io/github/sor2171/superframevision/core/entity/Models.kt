package io.github.sor2171.superframevision.core.entity

enum class Models(
    val label: String,
    val size: Pair<Int, Int>,
    val larger: Models?,
    val is16: Boolean
) {
    RIFE4_26_4(
        label = "rife-v4.26-4k",
        size = 3840 to 2176,
        larger = null,
        is16 = true
    ),
    RIFE4_26_1V(
        label = "rife-v4.26-1kv",
        size = 1088 to 1920,
        larger = RIFE4_26_4,
        is16 = true
    ),
    RIFE4_26(
        label = "rife-v4.26-1k",
        size = 1920 to 1088,
        larger = RIFE4_26_1V,
        is16 = true
    ),
    REAL_A3_2(
        label = "realesr-animevideov3-x2",
        size = 1920 to 1088,
        larger = null,
        is16 = false
    );

    fun toLarger(picSize: Pair<Int, Int>) : Models {
        if (larger == null) return this
        if (picSize.first > size.first || picSize.second > size.second) {
            return larger.toLarger(picSize)
        }
        return this
    }
}