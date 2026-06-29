package com.ch.core.base.event

/**
 * View 事件基类
 *
 * 用于 ViewModel 向 Activity/Fragment 发送一次性事件（如 Toast、导航、弹窗等）。
 * 使用 SharedFlow 传递，确保事件不丢失且不重复消费。
 *
 * 使用 interface 而非 sealed class，允许各 feature 模块定义自己的 sealed class 事件体系。
 *
 * 子类可继承此类定义具体事件，例如：
 * ```kotlin
 * sealed class LoginEvent : ViewEvent {
 *     data class ShowToast(val message: String) : LoginEvent()
 *     data class NavigateTo(val route: String) : LoginEvent()
 *     object LoginSuccess : LoginEvent()
 * }
 * ```
 */
interface ViewEvent {

    /**
     * 空操作事件
     *
     * 作为默认占位事件，无实际业务含义
     */
    data object NoOp : ViewEvent
}
