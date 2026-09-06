package io.github.sor2171.superframevision.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 应用设置服务：默认值 + 序列化读写。
 *
 * 与文件 IO 完全分离：
 * - 本服务只负责 `SettingsRepository.OverallSettings` 的 JSON 编解码与读写编排，不涉及任何平台文件细节；
 * - 落盘统一交给 [FileUtils]（通用文本文件服务，也可复用于其他文件）。
 */
object SettingsRepository {
    @Serializable
    data class OverallSettings(
        val themeColor: Int = 0,

        val upscaleThread: Int = 2,
        val inferThread: Int = 8,
    ) {
        companion object {
            val default: OverallSettings get() = OverallSettings()
        }
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val cached = MutableStateFlow<OverallSettings?>(null)
    val settings: StateFlow<OverallSettings?> = cached.asStateFlow()

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 初始化/读取设置。
     * 优先返回内存缓存；若未加载则去读磁盘。
     * 磁盘读不到或解析失败时，才会使用 [OverallSettings.default]。
     */
    suspend fun load(): OverallSettings = mutex.withLock {
        return withContext(Dispatchers.IO) {
            cached.value ?: fetchAndCache()
        }
    }

    /** 保存设置：更新内存 [cached] 并写入磁盘。 */
    fun save(value: OverallSettings) {
        cached.value = value
        repositoryScope.launch {
            mutex.withLock {
                val jsonString = json.encodeToString(value)
                FileUtils.write(jsonString, Const.CONFIG_FILE)
            }
        }
    }

    private suspend fun fetchAndCache(): OverallSettings =
        readFromStorage().also { cached.value = it }

    private suspend fun readFromStorage(): OverallSettings {
        val content = FileUtils.read(Const.CONFIG_FILE)
        return content?.toString(Charsets.UTF_8)?.let {
            runCatching {
                json.decodeFromString<OverallSettings>(it)
            }.getOrNull()
        } ?: OverallSettings.default.also { save(it) }
    }
}