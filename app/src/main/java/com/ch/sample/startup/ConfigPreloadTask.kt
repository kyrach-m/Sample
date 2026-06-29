package com.ch.sample.startup

import android.content.Context
import com.ch.core.cache.MemoryCache
import com.ch.core.common.logger.Logger
import com.ch.core.storage.kv.KVStorage
import com.ch.core.storage.kv.Scope
import com.ch.service.startup.dag.StartupTask

class ConfigPreloadTask : StartupTask() {

    override val name: String = "ConfigPreloadTask"

    override fun priority(): Int = 60

    override fun dependencies(): List<Class<out StartupTask>> {
        return listOf(DatabaseWarmUpTask::class.java)
    }

    override fun execute(context: Context) {
        val startTime = System.currentTimeMillis()

        val theme = KVStorage.getString("app_theme", "light", Scope.CONFIG)
        val language = KVStorage.getString("app_language", "auto", Scope.CONFIG)
        val pushEnabled = KVStorage.getString("push_enabled", "true", Scope.CONFIG)

        MemoryCache.put("config_theme", theme)
        MemoryCache.put("config_language", language)
        MemoryCache.put("config_push_enabled", pushEnabled)

        val duration = System.currentTimeMillis() - startTime
        Logger.d(TAG, "配置预加载完成，耗时: ${duration}ms")
    }

    companion object {
        private const val TAG = "ConfigPreloadTask"
    }
}
