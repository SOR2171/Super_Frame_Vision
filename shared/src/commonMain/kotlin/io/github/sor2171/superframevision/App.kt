package io.github.sor2171.superframevision

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import io.github.sor2171.superframevision.core.utils.SettingsRepository
import io.github.sor2171.superframevision.ui.component.ScreenButtonBar
import io.github.sor2171.superframevision.ui.component.ScreenButtonRail
import io.github.sor2171.superframevision.ui.screens.HomeScreen

@Composable
@Preview
fun App() {
    var currentScreen by rememberSaveable { mutableStateOf(Screens.Home) }
    val settings by SettingsRepository.settings.collectAsState()

    LaunchedEffect(Unit) {
        SettingsRepository.load()
    }

    @Composable
    fun NavigationRailItems() {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Screens.entries.forEach { screen ->
                ScreenButtonRail(
                    currentScreen = currentScreen,
                    screen = screen,
                    onClick = { currentScreen = screen }
                )
            }
        }
    }

    @Composable
    fun RowScope.NavigationBarItems() {
        Screens.entries.forEach { screen ->
            ScreenButtonBar(
                currentScreen = currentScreen,
                screen = screen,
                onClick = { currentScreen = screen }
            )
        }
    }

    val homeScreen = @Composable {
        HomeScreen()
    }

    MaterialTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val showNavigationRail = maxWidth > maxHeight

            Row(modifier = Modifier.fillMaxSize()) {
                if (showNavigationRail) {
                    NavigationRail(
                        containerColor = NavigationBarDefaults.containerColor
                    ) {
                        NavigationRailItems()
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    bottomBar = {
                        if (!showNavigationRail) {
                            NavigationBar {
                                NavigationBarItems()
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                Screens.Home -> homeScreen()
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}