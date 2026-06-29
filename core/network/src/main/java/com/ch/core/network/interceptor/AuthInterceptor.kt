package com.ch.core.network.interceptor

import com.ch.core.common.logger.Logger
import com.ch.core.network.token.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 认证拦截器
 *
 * 负责：1. 自动添加 Authorization Header
 * 2. 处理 401 响应自动刷新 Token
 * 3. 重试失败的请求
 *
 * ## 工作流程
 * 1. 请求前：从 TokenManager 获取 Token 并添加到 Header
 * 2. 如果 Token 即将过期（提前 5 分钟），先尝试刷新
 * 3. 如果收到 401，尝试刷新 Token 后重试
 * 4. 如果刷新失败，清除 Token 并通知业务层
 */
class AuthInterceptor : Interceptor {

    private val TAG = "AuthInterceptor"
    private val HEADER_AUTHORIZATION = "Authorization"
    private val MAX_RETRY_COUNT = 1 // 最多重试 1 次（刷新后重试）

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. 获取当前 Token
        var token = TokenManager.getAccessToken()

        // 2. 如果 Token 即将过期，先尝试刷新
        if (TokenManager.isTokenExpiringSoon()) {
            Logger.d(TAG, "Token 即将过期，尝试刷新...")
            val refreshed = runBlocking { TokenManager.refreshTokenIfNeeded() }
            if (refreshed) {
                token = TokenManager.getAccessToken()
                Logger.d(TAG, "Token 刷新成功，使用新 Token")
            } else {
                Logger.w(TAG, "Token 刷新失败，继续使用现有 Token")
            }
        }

        // 3. 构建带认证的请求
        val authenticatedRequest = if (token.isNotEmpty()) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        // 4. 执行请求
        var response = chain.proceed(authenticatedRequest)

        // 5. 如果收到 401，尝试刷新 Token 并重试
        if (response.code == 401) {
            Logger.d(TAG, "收到 401，尝试刷新 Token...")

            // 刷新 Token
            val refreshed = runBlocking { TokenManager.refreshTokenIfNeeded() }

            if (refreshed) {
                val newToken = TokenManager.getAccessToken()
                Logger.d(TAG, "Token 刷新成功，重试请求")

                // 关闭旧响应
                response.close()

                // 用新 Token 重试请求
                val retryRequest = originalRequest.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Bearer $newToken")
                    .build()
                response = chain.proceed(retryRequest)
            } else {
                Logger.w(TAG, "Token 刷新失败，401 未解决")
                // Token 刷新失败，可能是 Token 真正过期了
                // 业务层应该监听 TokenManager 的状态变化来处理登出
            }
        }

        return response
    }
}
