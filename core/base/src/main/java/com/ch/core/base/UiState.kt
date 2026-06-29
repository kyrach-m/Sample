package com.ch.core.base

/**
 * UI 状态密封类
 *
 * 统一封装页面加载状态，配合 StateFlow 驱动 UI 更新。
 *
 * 用法示例：
 * ```
 * val state: UiState<List<User>> = UiState.Loading
 * val state: UiState<List<User>> = UiState.Success(users)
 * val state: UiState<List<User>> = UiState.Error(404, "Not Found")
 * val state: UiState<List<User>> = UiState.Empty
 * ```
 *
 * @param T 数据类型
 */
sealed class UiState<out T> {

    /**
     * 加载中状态
     *
     * 通常用于显示 Loading 指示器
     */
    data object Loading : UiState<Nothing>()

    /**
     * 成功状态
     *
     * @param T 数据类型
     * @property data 成功加载的数据
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * 错误状态
     *
     * @property code 错误码
     * @property msg 错误信息
     */
    data class Error(val code: Int = -1, val msg: String = "Unknown error") : UiState<Nothing>()

    /**
     * 空数据状态
     *
     * 用于数据为空时显示空状态页面
     */
    data object Empty : UiState<Nothing>()

    /**
     * 是否处于加载状态
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * 是否成功
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * 是否失败
     */
    val isError: Boolean get() = this is Error

    /**
     * 是否为空
     */
    val isEmpty: Boolean get() = this is Empty
}
