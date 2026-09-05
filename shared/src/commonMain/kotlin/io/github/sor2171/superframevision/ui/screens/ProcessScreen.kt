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
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime

@Composable
fun ProcessScreen(
    cancelJob: () -> Unit,
    saverPickerLauncher: @Composable ((Path) -> Unit) -> SaverResultLauncher,
    platform: Platform,
    isProcessing: Boolean,
    queueFileList: SnapshotStateList<QueueFile>,
    consoleState: ConsoleState
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
                .height(160.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "软件运行于: ${platform.os} ${platform.architecture}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "这里，你将看到软件的内部输出",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = (if (isProcessing) "正在处理：" else "即将处理：")
                            + "${queueFileList.getOrNull(0)?.path ?: "队列为空"}",
                    style = MaterialTheme.typography.titleMedium
                )
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
            modifier = Modifier.weight(1f)
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
                modifier = Modifier.fillMaxSize()
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
                    Text("导出日志")
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { consoleState.clear() }
                ) {
                    Text("清空控制台")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = cancelJob,
                    enabled = isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "stop"
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
