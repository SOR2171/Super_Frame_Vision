package io.github.sor2171.superframevision.core.entity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import okio.Path

data class QueueFile(
    val path: Path,
    val processType: ProcessType,
    val isProcessing: MutableState<Boolean> = mutableStateOf(false)
)
