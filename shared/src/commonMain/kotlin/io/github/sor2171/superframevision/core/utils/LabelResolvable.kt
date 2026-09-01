package io.github.sor2171.superframevision.core.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

interface LabelResolvable {
    val label: StringResource

    @Composable
    fun label(): String {
        return stringResource(label)
    }
}