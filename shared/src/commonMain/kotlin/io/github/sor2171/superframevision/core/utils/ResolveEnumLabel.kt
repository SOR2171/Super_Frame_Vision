package io.github.sor2171.superframevision.core.utils

import androidx.compose.runtime.Composable
import io.github.sor2171.superframevision.core.entity.ProcessType
import io.github.sor2171.superframevision.core.entity.Screens
import org.jetbrains.compose.resources.stringResource


@Composable
fun Screens.label(): String {
    return stringResource(label)
}

@Composable
fun ProcessType.label(): String {
    return stringResource(label)
}