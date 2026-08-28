package io.github.sor2171.superframevision.core.utils

import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.currentPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun createFileUtils(): FileUtils = JvmFileUtils

fun resolveAppDataDirectory(): Path {
    val home = (System.getProperty("user.home")
        ?: throw IllegalStateException("user.home is null"))
        .toPath()

    return when (currentPlatform.os) {
        Platform.Os.Windows -> {
            val appData = System.getenv("APPDATA")
            val appDataPath = if (appData.isNullOrBlank()) {
                home / "AppData" / "Roaming"
            } else {
                appData.toPath()
            }
            appDataPath / Const.SHORT_APP_NAME
        }

        Platform.Os.MacOS ->
            home / "Library" / "Application Support" / Const.SHORT_APP_NAME

        Platform.Os.Linux -> {
            val xdg = System.getenv("XDG_CONFIG_HOME")
            val base = if (xdg.isNullOrBlank()) home / ".config" else xdg.toPath()
            base / Const.SHORT_APP_NAME
        }
    }
}

private object JvmFileUtils: FileUtils {
    private val fileSystem: FileSystem = FileSystem.SYSTEM

    override val appDataPath = resolveAppDataDirectory()

    override suspend fun read(vararg folders: String): String? {
        val targetPath = resolveTargetPath(*folders)
        return read(targetPath)
    }

    override suspend fun read(targetPath: Path): String? =
        withContext(Dispatchers.IO) {
            if (!fileSystem.exists(targetPath)) {
                return@withContext null
            }
            fileSystem.read(targetPath) {
                readUtf8()
            }
        }

    override suspend fun write(
        content: String,
        vararg folders: String
    ) {
        val targetPath = resolveTargetPath(*folders)
        write(content, targetPath)
    }

    override suspend fun write(content: String, targetPath: Path) {
        withContext(Dispatchers.IO) {
            targetPath.parent?.let { fileSystem.createDirectories(it) }

            // 在同目录下生成临时文件，避免写入中途中断损坏文件
            val tempPath = targetPath.parent
                ?.let { it / "${targetPath.name}.${System.nanoTime()}.tmp" }
                ?: throw IllegalArgumentException("targetPath is null")

            try {
                // 写入临时文件
                fileSystem.write(tempPath) {
                    writeUtf8(content)
                }
                // Okio 的 atomicMove 在支持的平台上会进行原子移动；不支持时会自动回退
                fileSystem.atomicMove(tempPath, targetPath)
            } finally {
                fileSystem.delete(tempPath, mustExist = false)
            }
        }
    }

    override suspend fun delete(vararg folders: String) {
        val targetPath = resolveTargetPath(*folders)
        delete(targetPath)
    }

    override suspend fun delete(targetPath: Path) {
        withContext(Dispatchers.IO) {
            fileSystem.delete(targetPath, mustExist = false)
        }
    }
}