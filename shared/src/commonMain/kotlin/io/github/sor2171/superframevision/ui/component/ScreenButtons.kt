package io.github.sor2171.superframevision.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.sor2171.superframevision.core.entity.Screens
import io.github.sor2171.superframevision.core.entity.title

@Composable
fun RowScope.ScreenButtonBar(
    currentScreen: Screens,
    screen: Screens,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = currentScreen == screen,
        onClick = onClick,
        icon = { Icon(screen.icon, contentDescription = screen.title()) },
        label = { Text(screen.title()) }
    )
}

@Composable
fun ScreenButtonRail(
    currentScreen: Screens,
    screen: Screens,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = currentScreen == screen,
        onClick = onClick,
        icon = { Icon(screen.icon, contentDescription = screen.title()) },
        label = { Text(screen.title()) }
    )
}

@Preview
@Composable
private fun ChangeScreenButtonPreview() {
    ScreenButtonRail(
        currentScreen = Screens.Home,
        screen = Screens.Home,
        onClick = {}
    )
}