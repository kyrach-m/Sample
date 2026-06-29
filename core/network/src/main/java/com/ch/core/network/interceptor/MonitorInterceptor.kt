package com.ch.core.network.interceptor

import com.ch.core.common.logger.Logger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.UUID

/**
 * 网络监控拦截器
 *
 * 为每个请求生成唯一 requestId，记录请求耗时、状态码等信息。
 * 通过 [NetworkMonitor] 回调上报数据，供 :service:logger 模块消费。
 *
 * 上报信息包括：
 * - requestId：唯一请求标识
 * - url：请求 URL
 * - method：请求方法
 * - duration：耗时（毫秒）
 * - statusCode：HTTP 状态码
 * - errorMessage：错误信息（如有）
 * - success：是否成功
 *
 * 用法示例：
 * ```kotlin
 * // 注册监听器
 * NetworkMonitor.addListener(object : NetworkMonitor.Listener {
 *     override fun onRequestComplete(report: NetworkMonitor.Report) {
 *         Logger.log("Request ${report.requestId}: ${report.url} ${report.duration}ms")
 *     }
 * })
 * ```
 */
class MonitorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        // 将 requestId 附加到请求 tag
        val taggedRequest = request.newBuilder()
            .tag(RequestIdTag::class.java, RequestIdTag(requestId))
            .build()

        var response: Response? = null
        var errorMessage: String? = null
        var statusCode = 0

        try {
            response = chain.proceed(taggedRequest)
            statusCode = response.code

            return response
        } catch (e: IOException) {
            errorMessage = e.message ?: "Network error"
            throw e
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val report = NetworkMonitor.Report(
                requestId = requestId,
                url = request.url.toString(),
                method = request.method,
                duration = duration,
                statusCode = statusCode,
                errorMessage = errorMessage,
                success = errorMessage == null && statusCode in 200..299
            )
            NetworkMonitor.notifyListeners(report)
        }
    }

    /**
     * 请求 ID 标签类
     *
     * 用于在 Request.tag 中存储 requestId
     */
    data class RequestIdTag(val requestId: String)
}

/**
 * 网络监控管理器
 *
 * 单例对象，管理网络请求监控数据的上报和监听。
 * 业务层（如 :service:logger）可通过 [addListener] 注册监听器消费数据。
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"

    /**
     * 监听器列表
     */
    private val listeners = mutableListOf<Listener>()

    /**
     * 网络请求报告数据类
     *
     * @property requestId 唯一请求标识
     * @property url 请求 URL
     * @property method 请求方法（GET/POST 等）
     * @property duration 请求耗时（毫秒）
     * @property statusCode HTTP 状态码（异常时为 0）
     * @property errorMessage 错误信息（成功时为 null）
     * @property success 是否成功
     */
    data class Report(
        val requestId: String,
        val url: String,
        val method: String,
        val duration: Long,
        val statusCode: Int,
        val errorMessage: String?,
        val success: Boolean
    )

    /**
     * 网络监控监听器接口
     *
     * 实现此接口以接收网络请求报告
     */
    fun interface Listener {
        /**
         * 请求完成回调
         *
         * @param report 请求报告数据
         */
        fun onRequestComplete(report: Report)
    }

    /**
     * 添加监听器
     *
     * @param listener 监听器
     */
    fun addListener(listener: Listener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    /**
     * 移除监听器
     *
     * @param listener 监听器
     */
    fun removeListener(listener: Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    /**
     * 通知所有监听器
     *
     * @param report 请求报告
     */
    internal fun notifyListeners(report: Report) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    listener.onRequestComplete(report)
                } catch (e: Exception) {
                    Logger.e(TAG, "Listener error: ${e.message}")
                }
            }
        }
    }

    /**
     * 清除所有监听器
     */
    fun clearListeners() {
        synchronized(listeners) {
            listeners.clear()
        }
    }
}
