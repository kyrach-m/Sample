package com.ch.core.network.cache

/**
 * 缓存策略枚举
 *
 * 定义网络请求的缓存行为，通过 Request.tag 传递给 CacheInterceptor。
 *
 * 用法示例：
 * ```kotlin
 * val request = Request.Builder()
 *     .url("https://api.example.com/data")
 *     .tag(CacheStrategy::class.java, CacheStrategy.CACHE_FIRST)
 *     .build()
 * ```
 */
enum class CacheStrategy {

    /**
     * 网络优先策略
     *
     * 优先请求网络，失败（网络异常/服务器错误）则读取缓存。
     * 适用于需要最新数据但允许降级的场景。
     */
    NETWORK_FIRST,

    /**
     * 缓存优先策略
     *
     * 优先读取缓存，缓存存在且未过期则直接返回，否则发起网络请求。
     * 适用于对实时性要求不高、优先保证速度的场景（如列表页）。
     */
    CACHE_FIRST,

    /**
     * 仅缓存策略
     *
     * 仅返回缓存数据，不发起网络请求。
     * 适用于离线模式或强制使用缓存的场景。
     */
    CACHE_ONLY
}
