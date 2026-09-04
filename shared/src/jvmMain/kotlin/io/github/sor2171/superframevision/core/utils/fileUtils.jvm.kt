package io.github.sor2171.superframevision.core.utils

import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.currentPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object FileUtils {
    actual val appDataPath: Path
        get() {
            val home = (System.getProperty("user.home")
                ?: throw IllegalStateException("user.home is null"))
                .toPath()

            return when (currentPlatform().os) {
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

    actual val basicTmpDir: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY

    /** only for Windows and AppImage */
    actual val installDir: Path
        get() {
            val platform = currentPlatform()
            if (platform.os == Platform.Os.Windows) {
                return File(
                    object {}.javaClass.protectionDomain.codeSource.location.toURI()
                ).parentFile.parentFile.absolutePath.toPath()
            } else if (platform.os == Platform.Os.Linux) {
                val appImagePath = System.getenv("APPIMAGE")
                if (appImagePath != null) {
                    return File(appImagePath).parentFile.absolutePath.toPath()
                }
            }
            return appDataPath
        }

    actual val installTmpDir: Path = installDir / Const.TEMP_DIR

    private val fileSystem: FileSystem = FileSystem.SYSTEM

    actual suspend fun read(targetPath: Path): ByteArray? =
        withContext(Dispatchers.IO) {
            if (!fileSystem.exists(targetPath)) {
                return@withContext null
            }
            fileSystem.read(targetPath) {
                readByteArray()
            }
        }

    actual suspend fun write(content: String, targetPath: Path) {
        withContext(Dispatchers.IO) {
            targetPath.parent?.let { fileSystem.createDirectories(it) }

            val tempPath = targetPath.parent
                ?.let { it / "${targetPath.name}.${System.nanoTime()}.tmp" }
                ?: throw IllegalArgumentException("targetPath is null")

            try {
                fileSystem.write(tempPath) {
                    writeUtf8(content)
                }
                fileSystem.atomicMove(tempPath, targetPath)
            } finally {
                fileSystem.delete(tempPath, mustExist = false)
            }
        }
    }

    actual fun delete(targetPath: Path) {
        fileSystem.delete(targetPath, mustExist = false)
    }

    actual fun getOutputStream(
        targetPath: Path,
        toUse: (BufferedSink) -> Unit
    ) {
        targetPath.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.sink(targetPath).buffer().use(toUse)
    }

    actual fun createDirectories(targetPath: Path) {
        fileSystem.createDirectories(targetPath)
    }

    actual fun list(targetPath: Path): List<Path> {
        FileUtils.createDirectories(targetPath)
        return fileSystem.list(targetPath)
    }

    actual fun move(sourcePath: Path, targetPath: Path) {
        fileSystem.createDirectories(targetPath.parent!!)
        fileSystem.atomicMove(sourcePath, targetPath)
    }

    actual suspend fun read(vararg folders: String): ByteArray? =
        read(resolveTargetPath(*folders))

    actual suspend fun write(content: String, vararg folders: String) =
        write(content, resolveTargetPath(*folders))

    actual fun delete(vararg folders: String) =
        delete(resolveTargetPath(*folders))

    actual fun createDirectories(vararg folders: String) =
        createDirectories(resolveTargetPath(*folders))

    actual fun list(vararg folders: String): List<Path> =
        list(resolveTargetPath(*folders))

    actual fun move(sourcePath: Path, vararg folders: String) =
        move(sourcePath, resolveTargetPath(*folders))

    actual fun getOutputStream(vararg folders: String, toUse: (BufferedSink) -> Unit) =
        getOutputStream(resolveTargetPath(*folders), toUse)
}