package com.ch.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 重试拦截器
 *
 * 当发生 IOException 时自动重试，采用指数退避策略。
 * 重试间隔：1s, 2s, 4s（2^n 秒）
 *
 * 注意：
 * - 仅对 IOException 重试（网络异常、超时等）
 * - 不对 HTTP 错误码重试（如 404、500 等）
 * - 最大重试次数可配置，默认 3 次
 *
 * 用法示例：
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addInterceptor(RetryInterceptor(maxRetries = 3))
 *     .build()
 * ```
 */
class RetryInterceptor(
    /**
     * 最大重试次数，默认 3 次
     */
    private val maxRetries: Int = DEFAULT_MAX_RETRIES
) : Interceptor {

    companion object {
        /**
         * 默认最大重试次数
         */
        private const val DEFAULT_MAX_RETRIES = 3

        /**
         * 基础延迟时间（毫秒）
         */
        private const val BASE_DELAY_MS = 1000L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        // 尝试 maxRetries + 1 次（首次请求 + 重试）
        for (attempt in 0..maxRetries) {
            try {
                // 如果不是第一次请求，先关闭上一次的响应
                lastResponse?.close()

                // 执行请求
                val response = chain.proceed(request)

                // 如果响应成功，直接返回
                if (response.isSuccessful) {
                    return response
                }

                // 保存响应，继续重试（针对 HTTP 错误码不重试，直接返回）
                lastResponse = response
                return response

            } catch (e: IOException) {
                lastException = e

                // 如果还有重试机会，等待后重试
                if (attempt < maxRetries) {
                    val delayMs = calculateDelay(attempt)
                    Thread.sleep(delayMs)
                }
            }
        }

        // 所有重试都失败，抛出最后一个异常
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    /**
     * 计算重试延迟时间（指数退避）
     *
     * 延迟公式：BASE_DELAY * 2^attempt
     * - attempt 0: 1s
     * - attempt 1: 2s
     * - attempt 2: 4s
     *
     * @param attempt 当前重试次数（从 0 开始）
     * @return 延迟时间（毫秒）
     */
    private fun calculateDelay(attempt: Int): Long {
        return BASE_DELAY_MS * (1L shl attempt) // 2^attempt * 1000ms
    }
}
