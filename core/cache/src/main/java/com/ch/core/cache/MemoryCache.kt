package com.ch.core.cache

import android.util.LruCache
import java.lang.ref.WeakReference

/**
 * 内存缓存（一级缓存）
 *
 * 基于 LruCache 实现，使用 WeakReference 包装值防止内存泄漏。
 * 最大容量为应用最大堆内存的 1/8。
 *
 * 特点：
 * - LRU 淘汰策略：最近最少使用的缓存自动淘汰
 * - WeakReference 包装：允许 GC 在内存紧张时回收缓存对象
 * - 线程安全：LruCache 内部已实现同步
 *
 * ## 与 ExpirableCache 的区别
 *
 * | 对比项 | MemoryCache（本类） | [ExpirableCache] |
 * |--------|---------------------|------------------|
 * | 过期机制 | 无（仅 LRU 淘汰 + GC 回收） | 支持 TTL 过期（精确到秒） |
 * | 存储内容 | 任意 Any 对象 | CacheEntry 包装的对象 |
 * | 主动清理 | 不支持 | 支持 [ExpirableCache.cleanExpired] |
 * | 适用场景 | 频繁访问的热点数据（如图片缩略图、用户信息） | 有时效性的缓存（如接口响应、会话数据） |
 *
 * ## 多级缓存架构
 * - 一级：MemoryCache / ExpirableCache（内存 LRU，毫秒级）
 * - 二级：KVStorage（磁盘 KV，毫秒级）
 * - 三级：Room（磁盘数据库，毫秒级）
 *
 * 用法示例：
 * ```kotlin
 * // 存入缓存
 * MemoryCache.put("user_123", userInfo)
 *
 * // 获取缓存
 * val user = MemoryCache.get("user_123") as? UserInfo
 *
 * // 移除缓存
 * MemoryCache.remove("user_123")
 *
 * // 清空缓存
 * MemoryCache.clear()
 * ```
 *
 * @see ExpirableCache 带 TTL 过期机制的缓存实现
 */
object MemoryCache {

    /**
     * 缓存最大容量（字节）
     *
     * 取应用最大堆内存的 1/8，避免 OOM。
     */
    private val MAX_SIZE: Int = (Runtime.getRuntime().maxMemory() / 8).toInt()

    /**
     * LRU 缓存实例
     *
     * 使用 WeakReference 包装值，允许 GC 在内存紧张时回收。
     * sizeOf 返回 0 表示不限制条目数量，仅受 WeakReference 和 LRU 策略控制。
     */
    private val cache = object : LruCache<String, WeakReference<Any>>(MAX_SIZE) {
        override fun sizeOf(key: String, value: WeakReference<Any>): Int {
            // 返回 0：不按字节计算大小，仅受 LRU 淘汰策略控制
            // WeakReference 允许 GC 在内存不足时回收值
            return 0
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: WeakReference<Any>,
            newValue: WeakReference<Any>?
        ) {
            // 缓存淘汰时的回调（可用于日志或清理）
            super.entryRemoved(evicted, key, oldValue, newValue)
        }
    }

    /**
     * 存入缓存
     *
     * 使用 WeakReference 包装值，防止内存泄漏。
     * 当 GC 运行时，WeakReference 引用的对象可能被回收。
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    fun put(key: String, value: Any) {
        cache.put(key, WeakReference(value))
    }

    /**
     * 获取缓存
     *
     * 从 WeakReference 中获取值。如果值已被 GC 回收，返回 null。
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在或已被回收返回 null
     */
    fun get(key: String): Any? {
        val weakRef = cache.get(key) ?: return null
        val value = weakRef.get()

        // 如果 WeakReference 已被回收，从缓存中移除
        if (value == null) {
            cache.remove(key)
        }

        return value
    }

    /**
     * 获取缓存（带类型转换）
     *
     * @param T 期望的类型
     * @param key 缓存键
     * @return 缓存值，如果不存在、类型不匹配或已被回收返回 null
     */
    inline fun <reified T> getAs(key: String): T? {
        return get(key) as? T
    }

    /**
     * 移除缓存
     *
     * @param key 缓存键
     * @return 被移除的值，如果不存在返回 null
     */
    fun remove(key: String): Any? {
        val weakRef = cache.remove(key)
        return weakRef?.get()
    }

    /**
     * 检查 key 是否存在
     *
     * 注意：即使返回 true，get 时仍可能因 GC 回收而返回 null。
     *
     * @param key 缓存键
     * @return 是否存在且未被回收
     */
    fun contains(key: String): Boolean {
        val weakRef = cache.get(key) ?: return false
        return weakRef.get() != null
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

    /**
     * 获取缓存最大容量
     *
     * @return 最大容量（字节）
     */
    fun maxSize(): Int {
        return cache.maxSize()
    }
}
