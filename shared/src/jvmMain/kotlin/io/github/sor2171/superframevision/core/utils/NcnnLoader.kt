package io.github.sor2171.superframevision.core.utils

import io.github.sor2171.superframevision.core.entity.Platform
import io.github.sor2171.superframevision.core.entity.currentPlatform
import kotlinx.io.IOException
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.security.MessageDigest

object NcnnLoader {

    private var loadedFilePath: String? = null
    private val loadLock = Any()
    private var isLoaded = false

    @Suppress("UnsafeDynamicallyLoadedCode")
    @Synchronized
    fun loadNcnn(): String {
        if (isLoaded) {
            return loadedFilePath ?: throw IllegalStateException("已加载但路径丢失")
        }

        val (resourcePath, suffix) = getPlatformLibraryInfo()

        val hash = computeResourceHash(resourcePath)
        val tempDir = System.getProperty("java.io.tmpdir")
        val tempFile = File(tempDir, "ncnn_$hash$suffix")

        val lockFile = File(tempDir, "ncnn_$hash.lock")
        lockFile.parentFile?.mkdirs()

        if (!tempFile.exists() || tempFile.length() == 0L) {
            RandomAccessFile(lockFile, "rw").use { raf ->
                raf.channel.lock().use { _ ->
                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        val inputStream = getResourceStream(resourcePath)
                            ?: throw IllegalStateException("资源未找到: $resourcePath")
                        inputStream.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (tempFile.length() == 0L) {
                            tempFile.delete()
                            throw IOException("写入的临时文件为空")
                        }
                    }
                }
            }
        }

        synchronized(loadLock) {
            if (!isLoaded) {
                System.load(tempFile.absolutePath)
                isLoaded = true
                loadedFilePath = tempFile.absolutePath
            }
        }

        return tempFile.absolutePath
    }

    private fun computeResourceHash(resourcePath: String): String {
        val inputStream = getResourceStream(resourcePath)
            ?: throw IllegalStateException("资源不存在: $resourcePath")

        return inputStream.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().joinToString("") { "%02x".format(it) }.take(16)
        }
    }

    private fun getResourceStream(resourcePath: String): InputStream? {
        return Thread.currentThread().contextClassLoader?.getResourceAsStream(resourcePath)
            ?: javaClass.getResourceAsStream(resourcePath)
            ?: ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath)
    }

    private fun getPlatformLibraryInfo(): Pair<String, String> {
        val platform = currentPlatform()

        return when (platform.os) {
            Platform.Os.Windows if platform.architecture == Platform.Architecture.X86_64
                -> "/natives/windows-x86_64/ncnn.dll" to ".dll"

            Platform.Os.MacOS if platform.architecture == Platform.Architecture.Arm64
                -> "/natives/macos-universal/ncnn.dylib" to ".dylib"

            Platform.Os.Linux if platform.architecture == Platform.Architecture.X86_64
                -> "/natives/linux-x86_64/libncnn.so" to ".so"

            else -> throw UnsupportedOperationException("Unsupported OS: ${platform.os}, architecture: ${platform.architecture}.")
        }
    }
}