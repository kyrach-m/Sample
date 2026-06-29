package com.ch.core.network.response

/**
 * API 响应密封类
 *
 * 统一封装网络请求结果，提供类型安全的成功/失败处理。
 *
 * 用法示例：
 * ```kotlin
 * when (val result = api.getUser()) {
 *     is ApiResponse.Success -> {
 *         val user = result.data
 *         // 处理成功数据
 *     }
 *     is ApiResponse.Failure -> {
 *         val code = result.code
 *         val msg = result.msg
 *         // 处理错误
 *     }
 * }
 * ```
 *
 * @param T 数据类型
 */
sealed class ApiResponse<out T> {

    /**
     * 成功响应
     *
     * @param T 数据类型
     * @property data 响应数据
     */
    data class Success<T>(val data: T) : ApiResponse<T>()

    /**
     * 失败响应
     *
     * @property code 错误码（HTTP 状态码或自定义错误码）
     * @property msg 错误信息
     */
    data class Failure(val code: Int, val msg: String) : ApiResponse<Nothing>()

    /**
     * 是否成功
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * 是否失败
     */
    val isFailure: Boolean get() = this is Failure

    /**
     * 获取成功数据，失败时返回 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    /**
     * 获取成功数据，失败时返回默认值
     *
     * @param defaultValue 默认值
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrDefault(defaultValue: T): T = when (this) {
        is Success -> data as? T ?: defaultValue
        is Failure -> defaultValue
    }

    /**
     * 链式转换成功数据
     *
     * @param transform 转换函数
     */
    inline fun <R> map(transform: (T) -> R): ApiResponse<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
}
