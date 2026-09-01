package io.github.sor2171.superframevision.core.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.sor2171.superframevision.core.utils.LabelResolvable
import org.jetbrains.compose.resources.StringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.home_sn
import superframevision.shared.generated.resources.info_sn
import superframevision.shared.generated.resources.process_sn
import superframevision.shared.generated.resources.settings_sn

enum class Screens(
    override val label: StringResource,
    val icon: ImageVector,
) : LabelResolvable {
    // SN for Screen Name
    Home(
        label = Res.string.home_sn,
        icon = Icons.Default.Home
    ),

    Process(
        label = Res.string.process_sn,
        icon = Icons.Default.Analytics
    ),

    Settings(
        label = Res.string.settings_sn,
        icon = Icons.Default.Settings
    ),

    Info(
        label = Res.string.info_sn,
        icon = Icons.Default.Info
    )
}
