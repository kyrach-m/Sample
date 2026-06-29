package com.ch.core.network.interceptor

import com.ch.core.network.cache.CacheStrategy
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 缓存拦截器（增强版）
 *
 * 支持三种缓存策略：NETWORK_FIRST / CACHE_FIRST / CACHE_ONLY。
 * 通过 Request.tag(CacheStrategy::class.java) 传递策略，未设置时默认使用 NETWORK_FIRST。
 *
 * 同时支持离线缓存读取（max-stale = 7 天）。
 *
 * 注意：需要在 OkHttpClient 中配置 Cache 目录和大小
 *
 * 用法示例：
 * ```kotlin
 * // 通过 Retrofit 的 Request 传递策略
 * @GET("data")
 * fun getData(@Tag strategy: CacheStrategy = CacheStrategy.CACHE_FIRST): Call<Data>
 *
 * // 或手动构建 Request
 * val request = Request.Builder()
 *     .url("https://api.example.com/data")
 *     .tag(CacheStrategy::class.java, CacheStrategy.CACHE_ONLY)
 *     .build()
 * ```
 */
class CacheInterceptor : Interceptor {

    companion object {
        /**
         * 缓存最大有效期（秒）
         * 离线时可读取 7 天内的缓存
         */
        private const val MAX_STALE_SECONDS = 7 * 24 * 60 * 60

        /**
         * 在线时缓存有效期（秒）
         */
        private const val MAX_AGE_SECONDS = 60
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val strategy = request.tag(CacheStrategy::class.java) ?: CacheStrategy.NETWORK_FIRST

        return when (strategy) {
            CacheStrategy.NETWORK_FIRST -> handleNetworkFirst(chain, request)
            CacheStrategy.CACHE_FIRST -> handleCacheFirst(chain, request)
            CacheStrategy.CACHE_ONLY -> handleCacheOnly(chain, request)
        }
    }

    /**
     * 网络优先策略
     *
     * 优先请求网络，失败则返回缓存（若有）
     */
    private fun handleNetworkFirst(chain: Interceptor.Chain, request: okhttp3.Request): Response {
        // 设置在线缓存策略
        val onlineRequest = request.newBuilder()
            .cacheControl(
                CacheControl.Builder()
                    .maxAge(MAX_AGE_SECONDS, TimeUnit.SECONDS)
                    .build()
            )
            .build()

        return try {
            val response = chain.proceed(onlineRequest)
            // 网络请求成功但非成功响应码时，尝试返回缓存
            if (!response.isSuccessful && response.cacheResponse != null) {
                response.close()
                return getCachedResponse(chain, request) ?: response
            }
            response
        } catch (e: IOException) {
            // 网络异常，尝试返回缓存
            getCachedResponse(chain, request) ?: throw e
        }
    }

    /**
     * 缓存优先策略
     *
     * 有缓存且未过期则直接返回，否则发起网络请求
     */
    private fun handleCacheFirst(chain: Interceptor.Chain, request: okhttp3.Request): Response {
        // 先尝试读取缓存
        val cacheRequest = request.newBuilder()
            .cacheControl(
                CacheControl.Builder()
                    .maxAge(MAX_AGE_SECONDS, TimeUnit.SECONDS)
                    .build()
            )
            .build()

        // 尝试获取缓存响应
        val cachedResponse = try {
            val cacheOnlyRequest = request.newBuilder()
                .cacheControl(
                    CacheControl.Builder()
                        .onlyIfCached()
                        .maxAge(MAX_AGE_SECONDS, TimeUnit.SECONDS)
                        .build()
                )
                .build()
            chain.proceed(cacheOnlyRequest)
        } catch (e: Exception) {
            null
        }

        // 如果有有效缓存，直接返回
        if (cachedResponse != null && cachedResponse.cacheResponse != null) {
            return cachedResponse
        }
        cachedResponse?.close()

        // 缓存无效，发起网络请求
        return chain.proceed(cacheRequest)
    }

    /**
     * 仅缓存策略
     *
     * 仅返回缓存，不发起网络请求
     */
    private fun handleCacheOnly(chain: Interceptor.Chain, request: okhttp3.Request): Response {
        val cacheOnlyRequest = request.newBuilder()
            .cacheControl(
                CacheControl.Builder()
                    .onlyIfCached()
                    .maxStale(MAX_STALE_SECONDS, TimeUnit.SECONDS)
                    .build()
            )
            .build()

        return chain.proceed(cacheOnlyRequest)
    }

    /**
     * 获取缓存响应
     *
     * @param chain 拦截器链
     * @param request 原始请求
     * @return 缓存响应，若无缓存返回 null
     */
    private fun getCachedResponse(chain: Interceptor.Chain, request: okhttp3.Request): Response? {
        return try {
            val cacheRequest = request.newBuilder()
                .cacheControl(
                    CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(MAX_STALE_SECONDS, TimeUnit.SECONDS)
                        .build()
                )
                .build()
            val response = chain.proceed(cacheRequest)
            if (response.cacheResponse != null) response else null
        } catch (e: Exception) {
            null
        }
    }
}
