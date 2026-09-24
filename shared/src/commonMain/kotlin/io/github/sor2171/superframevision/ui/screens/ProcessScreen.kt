package io.github.sor2171.superframevision.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.QueueFile
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.vinceglb.filekit.dialogs.compose.SaverResultLauncher
import kotlinx.coroutines.launch
import okio.Path
import org.jetbrains.compose.resources.stringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.process_btn_clear_console
import superframevision.shared.generated.resources.process_btn_export_log
import superframevision.shared.generated.resources.process_cd_stop
import superframevision.shared.generated.resources.process_internal_output_hint
import superframevision.shared.generated.resources.process_progress_idle
import superframevision.shared.generated.resources.process_progress_label
import superframevision.shared.generated.resources.process_queue_empty
import superframevision.shared.generated.resources.process_remaining_time
import superframevision.shared.generated.resources.process_running_on
import superframevision.shared.generated.resources.process_status_pending
import superframevision.shared.generated.resources.process_status_processing
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import kotlin.time.Duration

@Composable
fun ProcessScreen(
    cancelJob: () -> Unit,
    saverPickerLauncher: @Composable ((Path) -> Unit) -> SaverResultLauncher,
    platform: Platform,
    isProcessing: Boolean,
    queueFileList: SnapshotStateList<QueueFile>,
    consoleState: ConsoleState,
    ncnnTaskTotal: Int = 0,
    ncnnTaskCompleted: Int = 0,
    ncnnRemainingTime: Duration? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val launcher = saverPickerLauncher { path ->
        val logContent = consoleState.logLines.joinToString("\n")
        try {
            coroutineScope.launch {
                FileUtils.write(logContent, path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .padding(bottom = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(
                        Res.string.process_running_on,
                        platform.os,
                        platform.architecture
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(Res.string.process_internal_output_hint),
                    style = MaterialTheme.typography.bodyLarge
                )

                val currentFilePath = queueFileList.getOrNull(0)?.path
                    ?: stringResource(Res.string.process_queue_empty)
                val statusText = if (isProcessing) {
                    stringResource(Res.string.process_status_processing, currentFilePath)
                } else {
                    stringResource(Res.string.process_status_pending, currentFilePath)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progress = if (ncnnTaskTotal > 0) {
                        (ncnnTaskCompleted.toFloat() / ncnnTaskTotal.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val percent = (progress * 100).toInt()

                    val progressText = if (ncnnTaskTotal > 0) {
                        stringResource(Res.string.process_progress_label, ncnnTaskCompleted, ncnnTaskTotal, percent)
                    } else {
                        stringResource(Res.string.process_progress_idle)
                    }
                    val remainingText = stringResource(
                        Res.string.process_remaining_time,
                        formatDuration(ncnnRemainingTime)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = remainingText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val listState = rememberLazyListState()

        LaunchedEffect(consoleState.logLines.size) {
            if (consoleState.logLines.isNotEmpty()) {
                listState.animateScrollToItem(consoleState.logLines.size - 1)
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp)),
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(8.dp)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(consoleState.logLines) { line ->
                        Text(
                            text = line,
                            color = Color(0xFFD4D4D4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        launcher.launch(
                            suggestedName = "SFV_log_${LocalDateTime.now()}".replace(":", "-"),
                            defaultExtension = "log"
                        )
                    }
                ) {
                    Text(stringResource(Res.string.process_btn_export_log))
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { consoleState.clear() }
                ) {
                    Text(stringResource(Res.string.process_btn_clear_console))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = cancelJob,
                    enabled = isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(Res.string.process_cd_stop)
                    )
                }
            }
        }
    }
}

class ConsoleState(private val maxLines: Int = 1000) {
    val logLines = mutableStateListOf<String>()

    fun appendText(text: String) {
        val lines = text.split("\n")
        if (lines.isEmpty()) return

        if (logLines.isNotEmpty()) {
            val lastIndex = logLines.lastIndex
            logLines[lastIndex] = logLines[lastIndex] + lines.first()
            logLines.addAll(lines.drop(1))
        } else {
            logLines.addAll(lines)
        }

        while (logLines.size > maxLines) {
            logLines.removeAt(0)
        }
    }

    fun clear() {
        logLines.clear()
    }
}

@Composable
fun rememberConsoleState(redirectSystemOut: Boolean = true): ConsoleState {
    val consoleState = remember { ConsoleState() }

    LaunchedEffect(redirectSystemOut) {
        if (!redirectSystemOut) return@LaunchedEffect

        val originalOut = System.out
        val customOutputStream = object : OutputStream() {
            override fun write(b: Int) {
                val char = b.toChar().toString()
                kotlinx.coroutines.MainScope().launch {
                    consoleState.appendText(char)
                }
                originalOut.write(b)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                val text = String(b, off, len)
                kotlinx.coroutines.MainScope().launch {
                    consoleState.appendText(text)
                }
                originalOut.write(b, off, len)
            }
        }

        val printStream = PrintStream(customOutputStream, true, "UTF-8")
        System.setOut(printStream)
        System.setErr(printStream)
    }

    return consoleState
}

private fun formatDuration(duration: Duration?): String {
    if (duration == null) return "--:--"
    val totalSeconds = duration.inWholeSeconds
    if (totalSeconds < 0) return "--:--"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}