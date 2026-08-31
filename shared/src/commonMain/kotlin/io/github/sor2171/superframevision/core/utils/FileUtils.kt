package io.github.sor2171.superframevision.core.utils

import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.currentPlatform
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import java.io.File

/**
 * 平台无关的通用文本文件读写服务。
 *
 * 只负责单个文本文件的读 / 写 / 删，不包含任何设置、序列化等业务逻辑，
 * 可复用于设置、模型配置、缓存等任意需要落盘的文件。
 *
 * 各平台负责把文件放到"对应目录"，目前仅桌面端（JVM）实现，
 * 遵循各操作系统（Windows / Linux / macOS）的惯例解析配置目录。
 */
interface FileUtils {
    val appDataPath: Path
    val basicTmpDir: Path
        get() = FileSystem.SYSTEM_TEMPORARY_DIRECTORY

    /** only for Windows and AppImage */
    val installDir: Path
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

    val installTmpDir: Path
        get() = installDir / Const.TEMP_DIR

    fun resolveTargetPath(vararg folders: String): Path =
        folders.fold(appDataPath) { path, folder -> path / folder }

    /** 读取文件全部内容；文件不存在时返回 null。 */
    suspend fun read(vararg folders: String): String?
    suspend fun read(targetPath: Path): String?

    /** 覆盖写入内容，目录不存在时自动创建。 */
    suspend fun write(content: String, vararg folders: String)
    suspend fun write(content: String, targetPath: Path)

    /** 删除文件；文件不存在时静默成功。 */
    suspend fun delete(vararg folders: String)
    suspend fun delete(targetPath: Path)

    fun unzip(zipFile: Path, targetPath: Path) {
        val zipFs = FileSystem.SYSTEM.openZip(zipFile)

        fun extract(dir: Path) {
            for (entry in zipFs.list(dir)) {
                val relativePath = entry.toString().removePrefix("/")
                val destination = targetPath.resolve(relativePath)

                val metadata = zipFs.metadata(entry)
                if (metadata.isDirectory) {
                    FileSystem.SYSTEM.createDirectories(destination)
                    extract(entry)
                } else {
                    metadata.symlinkTarget?.let {
                        // 如果存在软链接处理
                        continue
                    }
                    destination.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
                    zipFs.source(entry).use { source ->
                        FileSystem.SYSTEM.sink(destination).buffer().use { sink ->
                            sink.writeAll(source)
                        }
                    }
                }
            }
        }

        extract("/".toPath())
    }
}

/**
 * 创建使用当前平台默认配置目录的 [FileUtils]，用于读写任意文本文件。
 */
expect fun createFileUtils(): FileUtils
