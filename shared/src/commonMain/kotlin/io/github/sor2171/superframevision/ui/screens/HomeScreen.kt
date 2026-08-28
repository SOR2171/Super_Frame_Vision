package io.github.sor2171.superframevision.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.currentPlatform

@Composable
fun HomeScreen(
    platform: Platform
) {

}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        platform = currentPlatform()
    )
}