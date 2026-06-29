package com.ch.core.network.interceptor

import com.ch.core.common.logger.Logger
import com.ch.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

/**
 * 日志拦截器
 *
 * 仅在 DEBUG 模式下打印完整请求信息和 curl 命令。
 * 便于调试和与后端联调。
 *
 * 打印内容包括：
 * - 请求 URL 和方法
 * - 请求头
 * - 请求体（如果有）
 * - 等效的 curl 命令
 * - 响应状态码和耗时
 *
 * 注意：生产环境应关闭此拦截器
 *
 * 用法示例：
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addInterceptor(LogInterceptor())
 *     .build()
 * ```
 */
class LogInterceptor : Interceptor {

    companion object {
        private const val TAG = "NetworkLog"
        private const val MAX_BODY_LENGTH = 4000 // Logcat 单条日志最大长度
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        // 非 DEBUG 模式直接返回
        if (!BuildConfig.DEBUG) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val startTime = System.currentTimeMillis()

        // 打印请求基本信息
        val logBuilder = StringBuilder()
        logBuilder.appendLine("╔═══════════════════════════════════════════════")
        logBuilder.appendLine("║ ${request.method} ${request.url}")
        logBuilder.appendLine("╠═══════════════════════════════════════════════")

        // 打印请求头
        if (request.headers.size > 0) {
            logBuilder.appendLine("║ Headers:")
            for (i in 0 until request.headers.size) {
                logBuilder.appendLine("║   ${request.headers.name(i)}: ${request.headers.value(i)}")
            }
        }

        // 打印请求体
        val body = request.body
        if (body != null) {
            val buffer = Buffer()
            body.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            logBuilder.appendLine("║ Body:")
            logBuilder.appendLine("║   $bodyString")
        }

        logBuilder.appendLine("╚═══════════════════════════════════════════════")

        // 打印 curl 命令
        logBuilder.appendLine("║ cURL:")
        logBuilder.appendLine("║   ${request.toCurl()}")
        logBuilder.appendLine("╚═══════════════════════════════════════════════")

        logWithLengthCheck(logBuilder.toString())

        // 执行请求
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        // 打印响应信息
        val responseLog = StringBuilder()
        responseLog.appendLine("╔═══════════════════════════════════════════════")
        responseLog.appendLine("║ Response: ${response.code} ${response.message}")
        responseLog.appendLine("║ URL: ${response.request.url}")
        responseLog.appendLine("║ Time: ${duration}ms")
        responseLog.appendLine("╚═══════════════════════════════════════════════")

        logWithLengthCheck(responseLog.toString())

        return response
    }

    /**
     * 将请求转换为 curl 命令
     *
     * @return curl 命令字符串
     */
    private fun okhttp3.Request.toCurl(): String {
        val curl = StringBuilder("curl -X $method")

        // 添加请求头
        for (i in 0 until headers.size) {
            curl.append(" -H '${headers.name(i)}: ${headers.value(i)}'")
        }

        // 添加请求体
        val body = body
        if (body != null) {
            val buffer = Buffer()
            body.writeTo(buffer)
            val bodyString = buffer.readUtf8().replace("'", "'\\''") // 转义单引号
            curl.append(" -d '$bodyString'")
        }

        curl.append(" '$url'")
        return curl.toString()
    }

    /**
     * 分段打印日志（避免 Logcat 截断）
     *
     * @param message 日志内容
     */
    private fun logWithLengthCheck(message: String) {
        if (message.length <= MAX_BODY_LENGTH) {
            Logger.d(TAG, message)
        } else {
            // 分段打印
            var start = 0
            while (start < message.length) {
                val end = minOf(start + MAX_BODY_LENGTH, message.length)
                Logger.d(TAG, message.substring(start, end))
                start = end
            }
        }
    }
}
