package io.github.sor2171.superframevision.core.utils

import io.github.sor2171.superframevision.core.utils.FileUtils.appDataPath
import okio.BufferedSink
import okio.Path

/**
 * 平台无关的通用文本文件读写服务。
 *
 * 只负责单个文本文件的读 / 写 / 删，不包含任何设置、序列化等业务逻辑，
 * 可复用于设置、模型配置、缓存等任意需要落盘的文件。
 *
 * 各平台负责把文件放到"对应目录"，目前仅桌面端（JVM）实现，
 * 遵循各操作系统（Windows / Linux / macOS）的惯例解析配置目录。
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object FileUtils {
    val appDataPath: Path
    val basicTmpDir: Path

    /** only for Windows and AppImage */
    val installDir: Path
    val installTmpDir: Path

    suspend fun read(targetPath: Path): ByteArray?
    suspend fun write(content: String, targetPath: Path)
    fun delete(targetPath: Path)
    fun createDirectories(targetPath: Path)
    fun list(targetPath: Path): List<Path>
    fun move(sourcePath: Path, targetPath: Path)
    fun getOutputStream(targetPath: Path, toUse: (BufferedSink) -> Unit)

    suspend fun read(vararg folders: String): ByteArray?
    suspend fun write(content: String, vararg folders: String)
    fun delete(vararg folders: String)
    fun createDirectories(vararg folders: String)
    fun list(vararg folders: String): List<Path>
    fun move(sourcePath: Path, vararg folders: String)
    fun getOutputStream(vararg folders: String, toUse: (BufferedSink) -> Unit)
}

fun resolveTargetPath(vararg folders: String): Path =
    folders.fold(appDataPath) { path, folder -> path / folder }