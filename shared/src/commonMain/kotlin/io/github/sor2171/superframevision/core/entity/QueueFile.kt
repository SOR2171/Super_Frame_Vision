package io.github.sor2171.superframevision.core.entity

import okio.Path

data class QueueFile(
    val path: Path,
    val processType: ProcessType,
    val isProcessing: Boolean = false
)
