package io.github.sor2171.superframevision.core.entity

import io.github.sor2171.superframevision.core.utils.FileUtils
import kotlinx.serialization.Serializable
import okio.Path
import org.jetbrains.compose.resources.StringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.working_dir_install_temp
import superframevision.shared.generated.resources.working_dir_system_temp

@Serializable
enum class WorkingDirType(
    override val label: StringResource,
) : LabelResolvable {
    SystemTemp(
        label = Res.string.working_dir_system_temp
    ),
    InstallDir(
        label = Res.string.working_dir_install_temp
    );

    fun getPath(): Path = when (this) {
        SystemTemp -> FileUtils.basicTmpDir
        InstallDir -> FileUtils.installTmpDir
    }
}
