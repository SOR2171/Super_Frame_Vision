package io.github.sor2171.superframevision.core.utils

import io.github.sor2171.superframevision.core.utils.FileUtils.appDataPath
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
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
    suspend fun copy(sourcePath: Path, targetPath: Path)
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

    fun clearTmp(targetDir: Path = basicTmpDir)
}

fun resolveTargetPath(vararg folders: String): Path =
    folders.fold(appDataPath) { path, folder -> path / folder }

fun Path.isFile(fileSystem: FileSystem = FileSystem.SYSTEM): Boolean {
    val metadata = fileSystem.metadataOrNull(this)
    return metadata?.isRegularFile == true
}

fun getFileHeadTailHash(
    path: Path,
    chunkSize: Long = 64 * 1024L,
    fileSystem: FileSystem = FileSystem.SYSTEM
): String? {
    if (!path.isFile(fileSystem)) return null
    return try {
        fileSystem.openReadOnly(path).use { handle ->
            val size = handle.size()
            val buffer = Buffer()
            if (size <= chunkSize * 2) {
                handle.read(0L, buffer, size)
            } else {
                handle.read(0L, buffer, chunkSize)
                handle.read(size - chunkSize, buffer, chunkSize)
            }
            buffer.readByteString().sha256().hex()
        }
    } catch (e: Exception) {
        println("Error computing hash for $path: ${e.message}")
        null
    }
}

fun isSameFile(
    file1: Path,
    file2: Path,
    chunkSize: Long = 64 * 1024L,
    fileSystem: FileSystem = FileSystem.SYSTEM
): Boolean {
    val meta1 = fileSystem.metadataOrNull(file1) ?: return false
    val meta2 = fileSystem.metadataOrNull(file2) ?: return false
    if (!meta1.isRegularFile || !meta2.isRegularFile) return false
    if (meta1.size != meta2.size) return false
    val hash1 = getFileHeadTailHash(file1, chunkSize, fileSystem) ?: return false
    val hash2 = getFileHeadTailHash(file2, chunkSize, fileSystem) ?: return false
    return hash1 == hash2
}