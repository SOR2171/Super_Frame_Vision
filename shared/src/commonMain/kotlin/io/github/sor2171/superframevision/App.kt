package io.github.sor2171.superframevision

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.github.sor2171.superframevision.core.entity.SettingsRepository

@Composable
@Preview
fun App() {
    val settings by SettingsRepository.settings.collectAsState()

    LaunchedEffect(Unit) {
        SettingsRepository.load()
    }

    MaterialTheme {
    }
}