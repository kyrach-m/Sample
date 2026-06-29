package com.ch.core.base.state

/**
 * 默认 UI 状态数据类
 *
 * 提供快速开发的默认状态类，适用于简单页面。
 * 复杂页面建议自定义 State 类以实现更精细的状态管理。
 *
 * 用法示例：
 * ```kotlin
 * class HomeViewModel : BaseViewModel<BaseState, ViewEvent>() {
 *     fun loadData() {
 *         launch {
 *             val data = repository.getData()
 *             setState(BaseState(isLoading = false, errorMsg = null))
 *         }
 *     }
 * }
 * ```
 *
 * @property isLoading 是否正在加载
 * @property errorMsg 错误信息，null 表示无错误
 * @property isEmpty 数据是否为空
 */
data class BaseState(
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isEmpty: Boolean = false
) {
    /**
     * 是否处于错误状态
     */
    val isError: Boolean get() = errorMsg != null

    /**
     * 是否处于正常状态（非加载、非错误、非空）
     */
    val isReady: Boolean get() = !isLoading && errorMsg == null && !isEmpty

    companion object {
        /**
         * 加载中状态
         */
        val Loading = BaseState(isLoading = true)

        /**
         * 空数据状态
         */
        val Empty = BaseState(isEmpty = true)

        /**
         * 创建错误状态
         *
         * @param msg 错误信息
         */
        fun error(msg: String) = BaseState(errorMsg = msg)
    }
}
