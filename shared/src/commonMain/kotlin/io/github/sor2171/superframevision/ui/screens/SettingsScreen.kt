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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sor2171.superframevision.core.utils.FileUtils
import io.github.sor2171.superframevision.core.utils.SettingsRepository.OverallSettings
import io.github.sor2171.superframevision.ui.component.NumberInputField
import io.github.sor2171.superframevision.ui.component.SettingItem

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
                    title = "清空缓存",
                    tooltipText = "它在 ${FileUtils.basicTmpDir}，不要在处理文件时清理。"
                ) {
                    Button(
                        onClick = {
                            FileUtils.list(FileUtils.basicTmpDir).forEach { folder ->
                                try {
                                    FileUtils.list(folder).forEach { file ->
                                        FileUtils.delete(file)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "clear cache"
                        )
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
                    title = "超分线程数",
                    tooltipText = "线程越多，设备要求越高，速度越快，质量不变"
                ) {
                    NumberInputField(
                        value = settings?.upscaleThread?.toString() ?: "",
                        onValueChange = {
                            var inputNumber = it.toIntOrNull() ?: 1
                            if (inputNumber < 1) inputNumber = 1
                            settings = settings?.copy(upscaleThread = inputNumber)
                        },
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }

                HorizontalDivider()

                SettingItem(
                    title = "插帧线程数",
                    tooltipText = "线程越多，设备要求越高，速度越快，质量不变"
                ) {
                    NumberInputField(
                        value = settings?.inferThread?.toString() ?: "",
                        onValueChange = {
                            var inputNumber = it.toIntOrNull() ?: 1
                            if (inputNumber < 1) inputNumber = 1
                            settings = settings?.copy(inferThread = inputNumber)
                        },
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }

//                SettingItem(
//                    title = "启用通知", tooltipText = "开启后将实时接收系统推送消息"
//                ) {
//                    Switch(
//                        checked = settings?.isTrue ?: false,
//                        onCheckedChange = { settings = settings?.copy(isTrue = it) })
//                }
//
//                HorizontalDivider()
//
//                SettingItem(
//                    title = "服务器地址", tooltipText = "填写 API 服务的基准 URL"
//                ) {
//                    OutlinedTextField(
//                        value = settings?.language ?: "Loading...",
//                        onValueChange = { settings = settings?.copy(language = it) },
//                        singleLine = true,
//                        modifier = Modifier.widthIn(max = 240.dp)
//                    )
//                }
//
//                HorizontalDivider()
//
//                // 3. 单选选项 (Radio)
//                SettingItem(
//                    title = "主题模式", tooltipText = "选择适合你的界面外观样式"
//                ) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        listOf("浅色", "深色", "跟随系统").forEach { theme ->
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(end = 8.dp)
//                            ) {
//                                RadioButton(
//                                    selected = (settings?.theme == theme),
//                                    onClick = { settings = settings?.copy(theme = theme) })
//                                Text(text = theme, style = MaterialTheme.typography.bodyMedium)
//                            }
//                        }
//                    }
//                }
//
//                HorizontalDivider()

                // 4. 多选选项 (Checkbox)
//                SettingItem(
//                    title = "数据同步项", tooltipText = "选择需要备份到云端的数据类型"
//                ) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        val items = listOf("相册", "文档", "设置")
//                        items.forEach { option ->
//                            val isChecked = settings.syncItems.contains(option)
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(end = 8.dp)
//                            ) {
//                                Checkbox(
//                                    checked = isChecked, onCheckedChange = { checked ->
//                                        val newList = settings.syncItems.toMutableList()
//                                        if (checked) newList.add(option) else newList.remove(option)
//                                        onSettingsChange(settings.copy(syncItems = newList))
//                                    })
//                                Text(text = option, style = MaterialTheme.typography.bodyMedium)
//                            }
//                        }
//                    }
//                }
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
                        contentDescription = "reset"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        settings?.let { confirmChange(it) }
                    }, enabled = settings != originSettings
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "save"
                    )
                }
            }
        }

    }
}