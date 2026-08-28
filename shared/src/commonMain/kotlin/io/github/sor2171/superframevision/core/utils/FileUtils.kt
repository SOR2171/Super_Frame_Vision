package io.github.sor2171.superframevision.core.utils

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
interface FileUtils {
    val appDataPath: Path

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
}

/**
 * 创建使用当前平台默认配置目录的 [FileUtils]，用于读写任意文本文件。
 */
expect fun createFileUtils(): FileUtils
