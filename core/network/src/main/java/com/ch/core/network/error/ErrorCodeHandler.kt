package com.ch.core.network.error

/**
 * API 错误码枚举
 *
 * 统一管理所有 API 错误码，便于全局搜索和修改。
 *
 * 错误码分类：
 * - 2xx：成功
 * - 4xx：客户端错误
 * - 5xx：服务器错误
 *
 * @property code HTTP 状态码或自定义错误码
 * @property description 错误描述
 */
enum class ApiErrorCode(val code: Int, val description: String) {
    // ========== 成功 ==========
    SUCCESS(200, "请求处理成功"),

    // ========== 客户端错误 ==========
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "认证失败"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    ACCOUNT_LOCKED(423, "账号锁定"),
    RATE_LIMITED(429, "请求过于频繁"),

    // ========== 服务器错误 ==========
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // ========== 业务自定义错误码 ==========
    TOKEN_EXPIRED(1001, "Token已过期"),
    ACCOUNT_KICKED(1002, "账号被踢"),
    INVALID_VERIFY_CODE(1003, "验证码错误"),
    USER_NOT_FOUND(1004, "用户不存在"),
    DEVICE_BINDING_FAILED(1005, "设备绑定失败"),

    // ========== 未知错误 ==========
    UNKNOWN(-1, "未知错误");

    companion object {
        /**
         * 根据错误码获取枚举值
         *
         * @param code 错误码
         * @return 对应的 ApiErrorCode，若未找到返回 UNKNOWN
         */
        fun fromCode(code: Int): ApiErrorCode {
            return entries.find { it.code == code } ?: UNKNOWN
        }

        /**
         * 判断是否为成功响应
         */
        fun isSuccess(code: Int): Boolean {
            return code == SUCCESS.code
        }

        /**
         * 判断是否为需要登录的认证错误
         */
        fun isAuthError(code: Int): Boolean {
            return code == UNAUTHORIZED.code || code == TOKEN_EXPIRED.code
        }

        /**
         * 判断是否为账号问题
         */
        fun isAccountError(code: Int): Boolean {
            return code == ACCOUNT_LOCKED.code || code == ACCOUNT_KICKED.code
        }
    }
}

/**
 * 错误码处理器接口
 *
 * 用于全局拦截和处理特定错误码。
 * 返回 true 表示已处理，上层不再重复处理。
 *
 * 用法示例：
 * ```kotlin
 * class CustomErrorHandler : ErrorCodeHandler {
 *     override fun handle(code: Int, msg: String): Boolean {
 *         return when (code) {
 *             ApiErrorCode.UNAUTHORIZED.code -> {
 *                 // Token 过期，刷新 Token 或跳转登录
 *                 true
 *             }
 *             else -> false
 *         }
 *     }
 * }
 *
 * // 注入处理器
 * BaseApi.setErrorHandler(CustomErrorHandler())
 * ```
 */
fun interface ErrorCodeHandler {

    /**
     * 处理错误码
     *
     * @param code 错误码（HTTP 状态码或业务错误码）
     * @param msg 错误信息
     * @return true 表示已处理，上层不再重复处理；false 表示未处理
     */
    fun handle(code: Int, msg: String): Boolean
}

/**
 * 网络连通性检查器接口
 *
 * 用于在 safeRequest 执行前检查网络状态。
 * 通过 [BaseApi.setConnectivityChecker] 注入实现。
 *
 * 注意：:core:network 不依赖 :core:common，通过接口解耦。
 * 业务层应在 Application 初始化时注入 :core:common 的 NetworkUtil：
 * ```kotlin
 * BaseApi.setConnectivityChecker { NetworkUtil.isConnected(context) }
 * ```
 */
fun interface ConnectivityChecker {

    /**
     * 检查网络是否已连接
     *
     * @return true 表示网络已连接
     */
    fun isConnected(): Boolean
}

/**
 * 默认错误码处理器
 *
 * 处理常见错误码：
 * - 401 / 1001：Token 过期 → 跳转登录页
 * - 1002：账号被踢 → 跳转登录页
 * - 423：账号锁定 → 提示用户
 * - 429：请求过于频繁 → 提示用户稍后再试
 * - 500：服务器内部错误 → 提示用户稍后再试
 *
 * 业务层可通过 [BaseApi.setErrorHandler] 注入自定义处理器覆盖默认行为。
 */
class DefaultErrorCodeHandler : ErrorCodeHandler {

    /**
     * Token 过期监听器
     *
     * 当检测到 Token 过期时回调
     */
    var onTokenExpired: (() -> Unit)? = null

    /**
     * 账号被踢监听器
     *
     * 当检测到账号被踢时回调
     */
    var onAccountKicked: (() -> Unit)? = null

    /**
     * 账号锁定监听器
     *
     * 当检测到账号被锁定时回调
     */
    var onAccountLocked: ((remainingTime: Long) -> Unit)? = null

    /**
     * 频繁请求监听器
     *
     * 当检测到请求过于频繁时回调
     */
    var onRateLimited: (() -> Unit)? = null

    /**
     * 服务器错误监听器
     *
     * 当检测到服务器内部错误时回调
     */
    var onServerError: (() -> Unit)? = null

    /**
     * 通用错误监听器
     *
     * 当检测到其他错误时回调（参数错误、资源不存在等）
     */
    var onError: ((code: Int, msg: String) -> Unit)? = null

    override fun handle(code: Int, msg: String): Boolean {
        val errorCode = ApiErrorCode.fromCode(code)

        return when (errorCode) {
            ApiErrorCode.SUCCESS -> true // 不应进入 handle

            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.TOKEN_EXPIRED -> {
                // Token 过期，跳转登录页
                onTokenExpired?.invoke()
                true
            }

            ApiErrorCode.ACCOUNT_KICKED -> {
                // 账号被踢，跳转登录页
                onAccountKicked?.invoke()
                true
            }

            ApiErrorCode.ACCOUNT_LOCKED -> {
                // 账号锁定，提示用户
                onAccountLocked?.invoke(0L)
                true
            }

            ApiErrorCode.RATE_LIMITED -> {
                // 请求过于频繁，提示用户稍后再试
                onRateLimited?.invoke()
                true
            }

            ApiErrorCode.INTERNAL_ERROR, ApiErrorCode.SERVICE_UNAVAILABLE -> {
                // 服务器错误，提示用户稍后再试
                onServerError?.invoke()
                true
            }

            ApiErrorCode.BAD_REQUEST,
            ApiErrorCode.FORBIDDEN,
            ApiErrorCode.NOT_FOUND,
            ApiErrorCode.INVALID_VERIFY_CODE,
            ApiErrorCode.USER_NOT_FOUND,
            ApiErrorCode.DEVICE_BINDING_FAILED -> {
                // 其他业务错误，交给上层处理
                onError?.invoke(code, msg)
                false
            }

            ApiErrorCode.UNKNOWN -> {
                // 未知错误，交给上层处理
                onError?.invoke(code, msg)
                false
            }
        }
    }
}
