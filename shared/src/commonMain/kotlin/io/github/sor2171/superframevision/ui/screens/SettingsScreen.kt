package io.github.sor2171.superframevision.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sor2171.superframevision.core.entity.VideoCodec
import io.github.sor2171.superframevision.core.entity.VideoFormat
import io.github.sor2171.superframevision.core.entity.VideoQuality
import io.github.sor2171.superframevision.core.entity.WorkingDirType
import io.github.sor2171.superframevision.core.service.NcnnRunner
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.SettingsRepository.OverallSettings
import io.github.sor2171.superframevision.ui.component.DropdownSelector
import io.github.sor2171.superframevision.ui.component.NumberInputField
import io.github.sor2171.superframevision.ui.component.SettingItem
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import okio.Path
import org.jetbrains.compose.resources.stringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.settings_btn_choose_dir
import superframevision.shared.generated.resources.settings_cd_clear_cache
import superframevision.shared.generated.resources.settings_cd_clear_output_dir
import superframevision.shared.generated.resources.settings_cd_reset
import superframevision.shared.generated.resources.settings_cd_save
import superframevision.shared.generated.resources.settings_output_dir_default
import superframevision.shared.generated.resources.settings_title_ai_device
import superframevision.shared.generated.resources.settings_title_clear_cache
import superframevision.shared.generated.resources.settings_title_infer_threads
import superframevision.shared.generated.resources.settings_title_theme_color
import superframevision.shared.generated.resources.settings_title_upscale_threads
import superframevision.shared.generated.resources.settings_title_video_codec
import superframevision.shared.generated.resources.settings_title_video_format
import superframevision.shared.generated.resources.settings_title_video_output_dir
import superframevision.shared.generated.resources.settings_title_video_quality
import superframevision.shared.generated.resources.settings_title_working_dir
import superframevision.shared.generated.resources.settings_tooltip_ai_device
import superframevision.shared.generated.resources.settings_tooltip_clear_cache
import superframevision.shared.generated.resources.settings_tooltip_theme_color
import superframevision.shared.generated.resources.settings_tooltip_threads
import superframevision.shared.generated.resources.settings_tooltip_video_codec
import superframevision.shared.generated.resources.settings_tooltip_video_format
import superframevision.shared.generated.resources.settings_tooltip_video_output_dir
import superframevision.shared.generated.resources.settings_tooltip_video_quality
import superframevision.shared.generated.resources.settings_tooltip_working_dir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    confirmChange: (OverallSettings) -> Unit,
    originSettings: OverallSettings?,
    settingsScreenScrollState: ScrollState,
    directoryPickerLauncher: @Composable ((Path) -> Unit) -> PickerResultLauncher
) {
    var settings by remember(originSettings) {
        mutableStateOf(originSettings?.copy())
    }

    val dirLauncher = directoryPickerLauncher { path ->
        settings = settings?.copy(videoOutputDir = path.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .padding(bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .weight(1f)
                .verticalScroll(settingsScreenScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentWorkingDir = settings?.workingDir ?: WorkingDirType.SystemTemp

                SettingItem(
                    title = stringResource(Res.string.settings_title_working_dir),
                    tooltipText = stringResource(Res.string.settings_tooltip_working_dir)
                ) {
                    DropdownSelector(
                        items = WorkingDirType.entries,
                        selectedItem = currentWorkingDir,
                        onItemSelected = { settings = settings?.copy(workingDir = it) },
                        selectedText = currentWorkingDir.label(),
                        modifier = Modifier.width(220.dp),
                        itemContent = { dirType ->
                            Column {
                                Text(
                                    text = dirType.label(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = dirType.getPath().toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }

                HorizontalDivider()

                val currentWorkingDirPath = currentWorkingDir.getPath()
                key(currentWorkingDirPath) {
                    SettingItem(
                        title = stringResource(Res.string.settings_title_clear_cache),
                        tooltipText = stringResource(
                            Res.string.settings_tooltip_clear_cache,
                            currentWorkingDirPath.toString()
                        )
                    ) {
                        Button(
                            onClick = {
                                FileUtils.clearTmp(currentWorkingDirPath)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.settings_cd_clear_cache)
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(
                    title = stringResource(Res.string.settings_title_theme_color),
                    tooltipText = stringResource(Res.string.settings_tooltip_theme_color)
                ) {
                    DropdownSelector(
                        items = Const.colorList.indices.toList(),
                        selectedItem = settings!!.themeColor,
                        onItemSelected = { settings = settings?.copy(themeColor = it) },
                        selectedText = Const.colorList[settings!!.themeColor].getColorHex(),
                        modifier = Modifier.width(160.dp),
                        itemContent = { index ->
                            val themes = Const.colorList[index]
                            Row(
                                modifier = Modifier,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    tint = themes.color,
                                    contentDescription = null
                                )
                                Text(text = themes.getColorHex())
                            }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxSize()
            ) {
                if (originSettings == null) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(128.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    return@Card
                }

                SettingItem(
                    title = stringResource(Res.string.settings_title_upscale_threads),
                    tooltipText = stringResource(Res.string.settings_tooltip_threads)
                ) {
                    NumberInputField(
                        value = settings?.upscaleThread?.toString() ?: "",
                        onValueChange = {
                            var inputNumber = it.toIntOrNull() ?: 1
                            if (inputNumber < 1) inputNumber = 1
                            settings = settings?.copy(upscaleThread = inputNumber)
                        },
                        modifier = Modifier.widthIn(max = 128.dp)
                    )
                }

                HorizontalDivider()

                SettingItem(
                    title = stringResource(Res.string.settings_title_infer_threads),
                    tooltipText = stringResource(Res.string.settings_tooltip_threads)
                ) {
                    NumberInputField(
                        value = settings?.inferThread?.toString() ?: "",
                        onValueChange = {
                            var inputNumber = it.toIntOrNull() ?: 1
                            if (inputNumber < 1) inputNumber = 1
                            settings = settings?.copy(inferThread = inputNumber)
                        },
                        modifier = Modifier.widthIn(max = 128.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(
                    title = stringResource(Res.string.settings_title_ai_device),
                    tooltipText = stringResource(Res.string.settings_tooltip_ai_device)
                ) {
                    val vulkanDevices = NcnnRunner.listVulkanDevices().let {
                        return@let if (it.isEmpty()) mutableListOf("CPU")
                        else {
                            it.addLast("CPU")
                            it
                        }
                    }
                    val currentDeviceIndex = if (settings!!.vulkanDevice == -1) {
                        vulkanDevices.lastIndex
                    } else {
                        settings!!.vulkanDevice
                    }

                    DropdownSelector(
                        items = vulkanDevices.indices.toList(),
                        selectedItem = currentDeviceIndex,
                        onItemSelected = { index ->
                            val finalIndex = if (index == vulkanDevices.size - 1) -1 else index
                            settings = settings?.copy(vulkanDevice = finalIndex)
                        },
                        selectedText = vulkanDevices.getOrElse(settings!!.vulkanDevice) { "CPU" },
                        modifier = Modifier.width(320.dp),
                        itemContent = { index ->
                            Text(text = "$index: ${vulkanDevices[index]}")
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentFormat = settings?.videoFormat ?: VideoFormat.MP4

                SettingItem(
                    title = stringResource(Res.string.settings_title_video_format),
                    tooltipText = stringResource(Res.string.settings_tooltip_video_format)
                ) {
                    DropdownSelector(
                        items = VideoFormat.entries,
                        selectedItem = currentFormat,
                        onItemSelected = { settings = settings?.copy(videoFormat = it) },
                        itemLabel = { it.label() },
                        modifier = Modifier.width(220.dp)
                    )
                }

                HorizontalDivider()

                val currentCodec = settings?.videoCodec ?: VideoCodec.LIBX265

                SettingItem(
                    title = stringResource(Res.string.settings_title_video_codec),
                    tooltipText = stringResource(Res.string.settings_tooltip_video_codec)
                ) {
                    DropdownSelector(
                        items = VideoCodec.entries,
                        selectedItem = currentCodec,
                        onItemSelected = { settings = settings?.copy(videoCodec = it) },
                        selectedText = currentCodec.label(),
                        modifier = Modifier.width(320.dp),
                        itemContent = { codec ->
                            Column {
                                Text(
                                    text = codec.label(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = codec.codecName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }

                HorizontalDivider()

                val currentQuality = settings?.videoQuality ?: VideoQuality.HIGH

                SettingItem(
                    title = stringResource(Res.string.settings_title_video_quality),
                    tooltipText = stringResource(Res.string.settings_tooltip_video_quality)
                ) {
                    DropdownSelector(
                        items = VideoQuality.entries,
                        selectedItem = currentQuality,
                        onItemSelected = { settings = settings?.copy(videoQuality = it) },
                        itemLabel = { it.label() },
                        modifier = Modifier.width(220.dp)
                    )
                }

                HorizontalDivider()

                val currentOutputDir = settings?.videoOutputDir
                SettingItem(
                    title = stringResource(Res.string.settings_title_video_output_dir),
                    tooltipText = stringResource(Res.string.settings_tooltip_video_output_dir)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { dirLauncher.launch() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = stringResource(Res.string.settings_btn_choose_dir)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.settings_btn_choose_dir))
                        }

                        OutlinedTextField(
                            value = currentOutputDir
                                ?: stringResource(Res.string.settings_output_dir_default),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.width(320.dp),
                            singleLine = true,
                            trailingIcon = if (!currentOutputDir.isNullOrEmpty()) {
                                {
                                    IconButton(
                                        onClick = {
                                            settings = settings?.copy(videoOutputDir = null)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(Res.string.settings_cd_clear_output_dir)
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        settings = originSettings
                    }, enabled = settings != originSettings
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.settings_cd_reset)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        settings?.let { confirmChange(it) }
                    },
                    enabled = settings != originSettings
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(Res.string.settings_cd_save)
                    )
                }
            }
        }
    }
}