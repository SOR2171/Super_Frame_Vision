package io.github.sor2171.superframevision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.sor2171.superframevision.core.utils.Const
import org.jetbrains.compose.resources.painterResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.sfv

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = Const.FULL_APP_NAME,
        state = rememberWindowState(width = 1100.dp, height = 800.dp),
        icon = painterResource(Res.drawable.sfv)
    ) {
        App()
    }
}