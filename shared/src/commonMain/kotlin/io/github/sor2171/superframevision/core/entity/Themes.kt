package io.github.sor2171.superframevision.core.entity

import androidx.compose.ui.graphics.Color

data class Themes(
//    override val label: StringResource,
    val color: Color
)
//    : LabelResolvable
{
    fun getColorHex() =
        "#" + (color.value shr 32).toUInt().toHexString().uppercase()
}
