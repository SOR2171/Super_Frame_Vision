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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sor2171.superframevision.core.service.NcnnRunner
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.SettingsRepository.OverallSettings
import io.github.sor2171.superframevision.ui.component.NumberInputField
import io.github.sor2171.superframevision.ui.component.SettingItem
import org.jetbrains.compose.resources.stringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.settings_cd_clear_cache
import superframevision.shared.generated.resources.settings_cd_reset
import superframevision.shared.generated.resources.settings_cd_save
import superframevision.shared.generated.resources.settings_title_ai_device
import superframevision.shared.generated.resources.settings_title_clear_cache
import superframevision.shared.generated.resources.settings_title_infer_threads
import superframevision.shared.generated.resources.settings_title_theme_color
import superframevision.shared.generated.resources.settings_title_upscale_threads
import superframevision.shared.generated.resources.settings_tooltip_ai_device
import superframevision.shared.generated.resources.settings_tooltip_clear_cache
import superframevision.shared.generated.resources.settings_tooltip_theme_color
import superframevision.shared.generated.resources.settings_tooltip_threads

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    confirmChange: (OverallSettings) -> Unit,
    originSettings: OverallSettings?,
    settingsScreenScrollState: ScrollState
) {
    var settings by remember(originSettings) {
        mutableStateOf(originSettings?.copy())
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
                SettingItem(
                    title = stringResource(Res.string.settings_title_clear_cache),
                    tooltipText = stringResource(
                        Res.string.settings_tooltip_clear_cache,
                        FileUtils.basicTmpDir
                    )
                ) {
                    Button(
                        onClick = {
                            FileUtils.clearTmp()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.settings_cd_clear_cache)
                        )
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
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = Const.colorList[settings!!.themeColor].getColorHex(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .width(160.dp)
                                .menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true
                                ),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Const.colorList.forEachIndexed { index, themes ->
                                DropdownMenuItem(
                                    text = {
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
                                    },
                                    onClick = {
                                        settings = settings?.copy(themeColor = index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
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
                    var expanded by remember { mutableStateOf(false) }
                    val vulkanDevices = NcnnRunner.listVulkanDevices().let {
                        return@let if (it.isEmpty()) mutableListOf("CPU")
                        else {
                            it.addLast("CPU")
                            it
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = vulkanDevices.getOrElse(settings!!.vulkanDevice) { "CPU" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .width(320.dp)
                                .menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true
                                ),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            vulkanDevices.forEachIndexed { index, device ->
                                DropdownMenuItem(
                                    text = { Text(text = "$index: $device") },
                                    onClick = {
                                        var index = index
                                        if (index == vulkanDevices.size - 1) index = -1
                                        settings = settings?.copy(vulkanDevice = index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

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