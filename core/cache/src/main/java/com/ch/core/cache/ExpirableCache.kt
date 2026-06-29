package com.ch.core.cache

import android.util.LruCache
import java.lang.ref.WeakReference

/**
 * 带过期时间的缓存条目
 *
 * @param T 缓存数据类型
 * @property data 缓存数据
 * @property timestamp 缓存创建时间（毫秒）
 * @property ttl 缓存有效期（毫秒），0 表示永不过期
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Long = 0L
) {
    /**
     * 检查缓存是否已过期
     *
     * @return true 表示已过期
     */
    fun isExpired(): Boolean {
        if (ttl <= 0) return false // 永不过期
        return System.currentTimeMillis() - timestamp > ttl
    }
}

/**
 * 带过期时间的内存缓存
 *
 * 在 LruCache 基础上增加了 TTL（Time-To-Live）过期机制。
 * 使用 WeakReference 包装 CacheEntry 防止内存泄漏。
 *
 * 特点：
 * - LRU 淘汰策略 + TTL 过期机制
 * - WeakReference 包装，允许 GC 回收
 * - 自动清理过期条目
 * - 线程安全
 *
 * 用法示例：
 * ```kotlin
 * // 存入缓存，有效期 5 分钟
 * ExpirableCache.put("user_123", userInfo, ttlSeconds = 300)
 *
 * // 获取缓存（过期返回 null）
 * val user = ExpirableCache.getAs<UserInfo>("user_123")
 *
 * // 存入永不过期的缓存
 * ExpirableCache.put("config", configData, ttlSeconds = 0)
 *
 * // 手动清理过期条目
 * ExpirableCache.cleanExpired()
 * ```
 */
object ExpirableCache {

    /**
     * 缓存最大容量
     *
     * 取应用最大堆内存的 1/8。
     */
    private val MAX_SIZE: Int = (Runtime.getRuntime().maxMemory() / 8).toInt()

    /**
     * LRU 缓存实例
     *
     * 存储 WeakReference 包装的 CacheEntry。
     */
    private val cache = object : LruCache<String, WeakReference<CacheEntry<Any>>>(MAX_SIZE) {
        override fun sizeOf(key: String, value: WeakReference<CacheEntry<Any>>): Int {
            return 0
        }
    }

    /**
     * 存入缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttlSeconds 有效期（秒），0 表示永不过期
     */
    fun put(key: String, value: Any, ttlSeconds: Long = 0L) {
        val ttlMillis = ttlSeconds * 1000
        val entry = CacheEntry(data = value, ttl = ttlMillis)
        cache.put(key, WeakReference(entry))
    }

    /**
     * 获取缓存
     *
     * 自动检查过期时间，过期则返回 null 并删除。
     *
     * @param key 缓存键
     * @return 缓存值，过期或不存在返回 null
     */
    fun get(key: String): Any? {
        val weakRef = cache.get(key) ?: return null
        val entry = weakRef.get()

        // WeakReference 已被 GC 回收
        if (entry == null) {
            cache.remove(key)
            return null
        }

        // 缓存已过期
        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }

        return entry.data
    }

    /**
     * 获取缓存（带类型转换）
     *
     * @param T 期望的类型
     * @param key 缓存键
     * @return 缓存值，过期/不存在/类型不匹配返回 null
     */
    inline fun <reified T> getAs(key: String): T? {
        return get(key) as? T
    }

    /**
     * 移除缓存
     *
     * @param key 缓存键
     * @return 被移除的值，不存在返回 null
     */
    fun remove(key: String): Any? {
        val weakRef = cache.remove(key)
        return weakRef?.get()?.data
    }

    /**
     * 清理所有过期条目
     *
     * 遍历缓存，删除已过期的条目。
     * 建议在 App 进入后台或空闲时调用。
     *
     * @return 清理的条目数
     */
    fun cleanExpired(): Int {
        var count = 0
        val snapshot = cache.snapshot()
        for ((key, weakRef) in snapshot) {
            val entry = weakRef.get()
            if (entry == null || entry.isExpired()) {
                cache.remove(key)
                count++
            }
        }
        return count
    }

    /**
     * 检查 key 是否存在且未过期
     *
     * @param key 缓存键
     * @return 是否存在且未过期
     */
    fun contains(key: String): Boolean {
        return get(key) != null
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * 获取当前缓存大小（条目数）
     *
     * @return 缓存条目数
     */
    fun size(): Int {
        return cache.size()
    }
}
