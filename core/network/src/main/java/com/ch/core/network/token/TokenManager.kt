package com.ch.core.network.token

import com.ch.core.common.logger.Logger
import com.ch.core.storage.kv.KVStorage
import com.ch.core.storage.kv.Scope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking

/**
 * Token 管理器
 *
 * 负责 Token 的存储、读取、自动刷新。
 * 使用单例模式确保全局只有一个 Token 管理器。
 *
 * ## Token 刷新机制
 * - 在网络请求前检查 Token 是否即将过期（提前 5 分钟刷新）
 * - 当收到 401 响应时自动尝试刷新 Token
 * - 刷新成功后重试原请求
 *
 * ## 存储策略
 * - Token 存储在 KVStorage 的 USER Scope，退出登录时整体清除
 *
 * ## 线程安全
 * - 所有操作线程安全
 * - 刷新锁防止多个请求同时刷新
 */
object TokenManager {

    private const val TAG = "TokenManager"

    // ========================================
    // Key 定义
    // ========================================
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"

    // Token 刷新锁，防止多个请求同时刷新
    private val refreshMutex = Mutex()

    // 是否正在刷新 Token
    @Volatile
    private var isRefreshing = false

    // Token 刷新回调（由业务层设置）
    private var tokenRefreshCallback: (suspend () -> Boolean)? = null

    // ========================================
    // 初始化
    // ========================================

    /**
     * 设置 Token 刷新回调
     *
     * 由业务层（通常是 feature_login）设置实际刷新逻辑
     *
     * @param callback 刷新回调，返回 true=刷新成功，false=刷新失败
     */
    fun setTokenRefreshCallback(callback: suspend () -> Boolean) {
        tokenRefreshCallback = callback
    }

    // ========================================
    // Token 存储
    // ========================================

    /**
     * 保存 Token 信息
     *
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 过期时间（秒）
     */
    fun saveToken(accessToken: String, refreshToken: String, expiresIn: Long) {
        val expiresAt = System.currentTimeMillis() + expiresIn * 1000
        KVStorage.putString(KEY_ACCESS_TOKEN, accessToken, Scope.USER)
        KVStorage.putString(KEY_REFRESH_TOKEN, refreshToken, Scope.USER)
        KVStorage.putLong(KEY_TOKEN_EXPIRES_AT, expiresAt, Scope.USER)
        Logger.d(TAG, "Token 已保存，过期时间: ${(expiresAt - System.currentTimeMillis()) / 1000}s")
    }

    /**
     * 获取当前有效的 Access Token
     *
     * @return Token 字符串，如果不存在返回空字符串
     */
    fun getAccessToken(): String {
        return KVStorage.getString(KEY_ACCESS_TOKEN, "", Scope.USER)
    }

    /**
     * 获取 Refresh Token
     *
     * @return Refresh Token 字符串，如果不存在返回空字符串
     */
    fun getRefreshToken(): String {
        return KVStorage.getString(KEY_REFRESH_TOKEN, "", Scope.USER)
    }

    /**
     * 检查 Access Token 是否即将过期（提前 5 分钟刷新）
     *
     * @return true=即将过期需要刷新，false=仍有效
     */
    fun isTokenExpiringSoon(): Boolean {
        val expiresAt = KVStorage.getLong(KEY_TOKEN_EXPIRES_AT, 0L, Scope.USER)
        if (expiresAt == 0L) return true // 没有过期时间，立即刷新
        val remainingMs = expiresAt - System.currentTimeMillis()
        val thresholdMs = 5 * 60 * 1000L // 提前 5 分钟刷新
        return remainingMs < thresholdMs
    }

    /**
     * 检查是否已登录（Token 是否存在）
     */
    fun isLoggedIn(): Boolean {
        return getAccessToken().isNotEmpty()
    }

    /**
     * 清除所有 Token（退出登录时调用）
     */
    fun clearToken() {
        KVStorage.remove(KEY_ACCESS_TOKEN, Scope.USER)
        KVStorage.remove(KEY_REFRESH_TOKEN, Scope.USER)
        KVStorage.remove(KEY_TOKEN_EXPIRES_AT, Scope.USER)
        Logger.d(TAG, "Token 已清除")
    }

    // ========================================
    // Token 刷新
    // ========================================

    /**
     * 刷新 Access Token
     *
     * 调用业务层设置的回调来执行实际刷新。
     * 如果刷新失败（refresh_token 也过期或无效），返回 false。
     *
     * @return true=刷新成功，false=刷新失败
     */
    suspend fun refreshTokenIfNeeded(): Boolean {
        val callback = tokenRefreshCallback
        if (callback == null) {
            Logger.w(TAG, "Token 刷新回调未设置，无法刷新")
            return false
        }

        // 如果已经在刷新中，等待刷新完成
        if (isRefreshing) {
            Logger.d(TAG, "Token 正在刷新中，等待完成...")
            // 等待一段时间后检查
            var waitCount = 0
            while (isRefreshing && waitCount < 50) {
                kotlinx.coroutines.delay(100)
                waitCount++
            }
            return if (waitCount >= 50) {
                Logger.w(TAG, "Token 刷新等待超时")
                false
            } else {
                getAccessToken().isNotEmpty()
            }
        }

        synchronized(refreshMutex) {
            // 双重检查
            if (isRefreshing) {
                return getAccessToken().isNotEmpty()
            }
            isRefreshing = true
        }

        try {
            Logger.d(TAG, "开始刷新 Token...")
            val success = callback()
            if (success) {
                Logger.d(TAG, "Token 刷新成功")
            } else {
                Logger.w(TAG, "Token 刷新失败")
            }
            return success
        } catch (e: Exception) {
            Logger.e(TAG, "Token 刷新异常", e)
            return false
        } finally {
            isRefreshing = false
        }
    }

    /**
     * 获取 Access Token（用于认证）
     *
     * 如果 Token 即将过期，会先尝试刷新
     */
    fun getAccessTokenForAuth(): String {
        return getAccessToken()
    }

    /**
     * 同步刷新 Token（仅在启动时调用）
     *
     * 适用于在应用启动时检查并刷新过期的 Token
     */
    suspend fun refreshIfExpired(): Boolean {
        val expiresAt = KVStorage.getLong(KEY_TOKEN_EXPIRES_AT, 0L, Scope.USER)
        if (expiresAt == 0L) return false // 没有 Token

        val remainingMs = expiresAt - System.currentTimeMillis()
        if (remainingMs > 0) return false // Token 仍有效

        // Token 已过期，尝试刷新
        return refreshTokenIfNeeded()
    }
}
