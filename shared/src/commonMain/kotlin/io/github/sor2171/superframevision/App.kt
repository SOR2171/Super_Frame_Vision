package io.github.sor2171.superframevision

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme
import io.github.sor2171.superframevision.core.entity.ProcessType
import io.github.sor2171.superframevision.core.entity.QueueFile
import io.github.sor2171.superframevision.core.entity.Screens
import io.github.sor2171.superframevision.core.entity.currentPlatform
import io.github.sor2171.superframevision.core.service.ProcessLauncher
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import io.github.sor2171.superframevision.ui.screens.HomeScreen
import io.github.sor2171.superframevision.ui.screens.InfoScreen
import io.github.sor2171.superframevision.ui.screens.ProcessScreen
import io.github.sor2171.superframevision.ui.screens.SettingsScreen
import io.github.sor2171.superframevision.ui.screens.rememberConsoleState
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath

@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screens.Home) }
    var chosenProcessType by remember { mutableStateOf(ProcessType.VideoFI) }
    var isProcessing by remember { mutableStateOf(false) }
    val queueFileList = remember { mutableStateListOf<QueueFile>() }

    val settings by SettingsRepository.settings.collectAsState()
    val settingsScreenScrollState = rememberScrollState()
    val consoleState = rememberConsoleState()

    val seedColor = Const.colorList[settings?.themeColor ?: 0].color
    val platform = currentPlatform()

    fun usableSettings() = settings ?: SettingsRepository.OverallSettings.default

    val processor = remember(coroutineScope, queueFileList) {
        ProcessLauncher(
            scope = coroutineScope,
            queueFileList = queueFileList,
            getSettings = ::usableSettings,
            getProcessType = { chosenProcessType },
            onProcessingStateChange = { isProcessing = it }
        )
    }

    LaunchedEffect(Unit) {
        SettingsRepository.load()
    }

    val filePickerLauncher = @Composable { callback: (Path) -> Unit ->
        rememberFilePickerLauncher(
            mode = FileKitMode.Multiple()
        ) { files ->
            files?.forEach { file ->
                val path = file.path.toPath()
                callback(path)
            } ?: return@rememberFilePickerLauncher
        }
    }

    val saverPickerLauncher = @Composable { callback: (Path) -> Unit ->
        rememberFileSaverLauncher(
            dialogSettings = FileKitDialogSettings.createDefault(),
            onError = {}
        ) { file ->
            val path = file?.path?.toPath() ?: return@rememberFileSaverLauncher
            callback(path)
        }
    }

    val colorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isSystemInDarkTheme()
    )

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            NavigationRail(
                containerColor = NavigationBarDefaults.containerColor
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.fillMaxHeight(0.1f))
                    Screens.entries.forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon, contentDescription = screen.label()
                                )
                            },
                            label = { Text(screen.label()) })
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        Screens.Home -> HomeScreen(
                            addProcessQueue = { queueFileList.add(it) },
                            removeQueueFile = { queueFileList.remove(it) },
                            changeProcessType = { chosenProcessType = it },
                            cancelJob = processor::cancel,
                            processStart = processor::start,
                            queueFileList = queueFileList,
                            filePickerLauncher = filePickerLauncher,
                            chosenProcessType = chosenProcessType,
                            isProcessing = isProcessing
                        )

                        Screens.Process -> ProcessScreen(
                            cancelJob = processor::cancel,
                            saverPickerLauncher = saverPickerLauncher,
                            isProcessing = isProcessing,
                            platform = platform,
                            queueFileList = queueFileList,
                            consoleState = consoleState
                        )

                        Screens.Settings -> SettingsScreen(
                            settingsScreenScrollState = settingsScreenScrollState,
                            confirmChange = SettingsRepository::save,
                            originSettings = settings
                        )

                        Screens.Info -> InfoScreen()
                    }
                }
            }
        }
    }
}