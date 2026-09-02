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
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import io.github.sor2171.superframevision.ui.screens.HomeScreen
import io.github.sor2171.superframevision.ui.screens.InfoScreen
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath

@Composable
@Preview
fun App() {
    var currentScreen by rememberSaveable { mutableStateOf(Screens.Home) }
    var seedColor by remember { mutableStateOf(Const.colorList[0]) }
    var chosenProcessType by rememberSaveable { mutableStateOf(ProcessType.VideoFI) }

    val queueFileList = remember { mutableStateListOf<QueueFile>() }

    val settings by SettingsRepository.settings.collectAsState()
    val settingsScreenScrollState = rememberScrollState()
    val platform = currentPlatform()

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

    val processStart = {

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
                                    imageVector = screen.icon,
                                    contentDescription = screen.label()
                                )
                            },
                            label = { Text(screen.label()) }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
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
                            changeProcessType = { chosenProcessType = it},
                            processStart = processStart,
                            queueFileList = queueFileList,
                            filePickerLauncher = filePickerLauncher,
                            chosenProcessType = chosenProcessType
                        )

                        Screens.Process -> InfoScreen()

                        Screens.Settings -> InfoScreen()

                        Screens.Info -> InfoScreen()
                    }
                }
            }
        }
    }
}