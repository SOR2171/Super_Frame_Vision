package io.github.sor2171.superframevision.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.currentPlatform
import io.github.sor2171.superframevision.core.utils.SettingsRepository.OverallSettings
import io.github.sor2171.superframevision.ui.component.ScrollColumn

@Composable
fun HomeScreen(
    platform: Platform,
    settings: OverallSettings?,
) {
    val scrollState = rememberScrollState()
    ScrollColumn(
        scrollState = scrollState,
    ) {
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        platform = currentPlatform(),
        settings = OverallSettings(),
//        settings = null,
    )
}