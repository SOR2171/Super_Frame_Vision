package io.github.sor2171.superframevision.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.sor2171.superframevision.core.entity.ProcessType
import io.github.sor2171.superframevision.core.entity.QueueFile
import io.github.sor2171.superframevision.core.utils.label
import io.github.sor2171.superframevision.ui.component.ScrollColumn
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.choose_file_button
import superframevision.shared.generated.resources.hint_before_start
import superframevision.shared.generated.resources.kmp
import java.io.File
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    addProcessQueue: (QueueFile) -> Unit,
    removeQueueFile: (QueueFile) -> Unit,
    changeProcessType: (ProcessType) -> Unit,
    queueFileList: List<QueueFile>,
    filePickerLauncher: @Composable ((Path) -> Unit) -> PickerResultLauncher,
    chosenProcessType: ProcessType,
) {
    var isDragging by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val animatedElevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 500)
    )

    val pathToQueueFile = { path: Path ->
        QueueFile(
            path = path,
            processType = chosenProcessType
        )
    }

    val launcher = filePickerLauncher { path ->
        addProcessQueue(pathToQueueFile(path))
    }

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isDragging = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragging = false
                val files = (event.dragData() as? DragData.FilesList)?.readFiles()
                val filePaths = files?.map { File(URI(it)).absolutePath.toPath() }
                filePaths?.forEach { path ->
                    addProcessQueue(pathToQueueFile(path))
                }
                return false
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .padding(bottom = 16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .height(256.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Image(
                        modifier = Modifier
                            .height(64.dp)
                            .width(64.dp)
                            .align(Alignment.End),
                        painter = painterResource(Res.drawable.kmp),
                        contentDescription = "Logo",
                        alpha = 0.8f
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .align(Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "欢迎使用SFV：",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "现在，先选择处理方法，再选择文件，最后点击开始，别忘了查看设置。",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                val strokeColor = MaterialTheme.colorScheme.primary
                Card(
                    modifier = Modifier
                        .padding(12.dp)
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { true },
                            target = dragAndDropTarget
                        )
                        .drawBehind {
                            drawRoundRect(
                                color = strokeColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(10.dp.toPx(), 8.dp.toPx()),
                                        phase = 0f
                                    )
                                ),
                                cornerRadius = CornerRadius(10.dp.toPx())
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    elevation = CardDefaults.cardElevation(animatedElevation)
                ) {
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        AnimatedContent(
                            targetState = isDragging,
                            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                            label = "DraggingCard"
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (it) {
                                    Icon(
                                        modifier = Modifier.fillMaxSize(0.3f),
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Files",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    FilledTonalButton(
                                        modifier = Modifier.padding(8.dp),
                                        onClick = { launcher.launch() },
                                    ) {
                                        Text(text = stringResource(Res.string.choose_file_button))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .padding(16.dp)
                .height(64.dp)
                .fillMaxWidth()
        ) {
            Column {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                ) {
                    ProcessType.entries.toList().forEachIndexed { index, processType ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ProcessType.entries.size
                            ),
                            onClick = { changeProcessType(processType) },
                            selected = chosenProcessType == processType,
                            label = { Text(processType.label()) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedCard(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            ScrollColumn(
                scrollState = scrollState,
            ) {
                queueFileList.forEach { queueFile ->
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = queueFile.path.toString())
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                modifier = Modifier.padding(4.dp),
                                onClick = { removeQueueFile(queueFile) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "delete"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .padding(16.dp)
                .height(72.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(Res.string.hint_before_start))

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Process",
                    )
                }
            }
        }
    }
}