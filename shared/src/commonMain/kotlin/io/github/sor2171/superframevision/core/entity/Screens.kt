package io.github.sor2171.superframevision.core.entity

import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.home_sn
import superframevision.shared.generated.resources.info_sn
import superframevision.shared.generated.resources.process_sn

enum class Screens(
    val titleRes: StringResource,
    val icon: ImageVector
) {
    // SN for Screen Name
    Home(
        titleRes = Res.string.home_sn,
        icon = androidx.compose.material.icons.Icons.Default.Home
    ),

    Process(
        titleRes = Res.string.process_sn,
        icon = androidx.compose.material.icons.Icons.Default.Analytics
    ),

    Info(
        titleRes = Res.string.info_sn,
        icon = androidx.compose.material.icons.Icons.Default.Info
    )
}

@Composable
fun Screens.title(): String {
    return stringResource(titleRes)
}
