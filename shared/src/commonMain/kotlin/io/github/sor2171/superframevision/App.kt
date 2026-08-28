package io.github.sor2171.superframevision

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.sor2171.superframevision.core.entity.Screens
import io.github.sor2171.superframevision.core.entity.currentPlatform
import io.github.sor2171.superframevision.core.entity.title
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import io.github.sor2171.superframevision.ui.screens.HomeScreen

@Composable
@Preview
fun App() {
    var currentScreen by rememberSaveable { mutableStateOf(Screens.Home) }
    val settings by SettingsRepository.settings.collectAsState()
    val platform = currentPlatform()

    LaunchedEffect(Unit) {
        SettingsRepository.load()
    }

    MaterialTheme {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            NavigationRail(
                containerColor = NavigationBarDefaults.containerColor
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Screens.entries.forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title()) },
                            label = { Text(screen.title()) }
                        )
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
                            platform = platform
                        )

                        Screens.Process -> HomeScreen(
                            platform = platform
                        )

                        Screens.Settings -> HomeScreen(
                            platform = platform
                        )

                        Screens.Info -> HomeScreen(
                            platform = platform
                        )
                    }
                }
            }
        }
    }
}