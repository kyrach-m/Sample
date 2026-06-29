package com.ch.core.base

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ch.core.base.event.ViewEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel 基类（商业级增强版）
 *
 * 提供双泛型支持：State（页面状态）+ Event（一次性事件）。
 * 内置 SavedStateHandle 支持进程销毁后数据恢复。
 * 封装 execute 高阶函数，自动管理 Loading/Success/Error 状态流转。
 *
 * 用法示例：
 * ```kotlin
 * // 定义状态和事件
 * data class LoginState(val isLoggedIn: Boolean = false)
 * sealed class LoginEvent : ViewEvent() {
 *     data class ShowToast(val msg: String) : LoginEvent()
 * }
 *
 * // 继承使用
 * class LoginViewModel(
 *     savedStateHandle: SavedStateHandle
 * ) : BaseViewModel<LoginState, LoginEvent>(savedStateHandle) {
 *
 *     fun login(username: String, password: String) {
 *         launch {
 *             execute(
 *                 block = { repository.login(username, password) },
 *                 onSuccess = { sendEvent(LoginEvent.ShowToast("登录成功")) }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @param State 页面状态类型
 * @param Event 事件类型，必须是 ViewEvent 的子类
 * @property savedStateHandle 用于进程销毁恢复的句柄
 */
abstract class BaseViewModel<State, Event : ViewEvent>(
    protected val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * 无参构造（兼容不需要 SavedStateHandle 的场景）
     */
    constructor() : this(SavedStateHandle())

    /**
     * 日志标签
     *
     * 默认返回当前类的简单名称，子类可覆写自定义。
     * 用于 Logger 日志输出时标识来源。
     */
    protected val loggerTag: String
        get() = this::class.java.simpleName

    // ==================== State 管理 ====================

    /**
     * 内部可变状态流
     */
    private val _state = MutableStateFlow<State?>(null)

    /**
     * 对外暴露的只读状态流
     *
     * 在 Activity/Fragment 中通过 collect 观察状态变化
     */
    val state: StateFlow<State?> = _state.asStateFlow()

    // ==================== Event 管理 ====================

    /**
     * 内部事件流
     *
     * extraBufferCapacity = 64 防止事件丢失
     * onBufferOverflow = DROP_OLDEST 缓冲区满时丢弃最旧事件
     */
    protected val _event = MutableSharedFlow<Event>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    /**
     * 对外暴露的只读事件流
     *
     * 在 Activity/Fragment 中通过 collect 消费事件
     */
    val event: SharedFlow<Event> = _event.asSharedFlow()

    // ==================== 异常处理 ====================

    /**
     * 协程异常处理器
     *
     * 捕获未处理异常，记录日志
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onException(throwable)
    }

    // ==================== 核心方法 ====================

    /**
     * 启动协程
     *
     * 自动绑定 viewModelScope，内部处理异常。
     *
     * @param block 挂起函数
     */
    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            try {
                block()
            } catch (e: Exception) {
                onException(e)
            }
        }
    }

    /**
     * 执行高阶函数
     *
     * 自动管理状态流转：Loading → Success/Error。
     * 异常时自动发送错误事件。
     *
     * 用法示例：
     * ```kotlin
     * fun loadData() {
     *     launch {
     *         execute(
     *             block = { repository.getData() },
     *             onSuccess = { data -> setState(MyState(data)) }
     *         )
     *     }
     * }
     * ```
     *
     * @param State 返回的数据类型
     * @param block 业务逻辑挂起函数
     * @param onSuccess 成功回调，接收返回的数据
     */
    protected suspend fun execute(
        block: suspend () -> State,
        onSuccess: (State) -> Unit = {}
    ) {
        try {
            setLoading()
            val result = block()
            onSuccess(result)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
            onSendToastEvent(e.message ?: "Unknown error")
        }
    }

    /**
     * 设置状态
     *
     * @param newState 新的 UI 状态
     */
    protected fun setState(newState: State) {
        _state.value = newState
    }

    /**
     * 设置加载状态
     *
     * 子类可重写此方法自定义 Loading 状态表现
     */
    protected open fun setLoading() {
        // 默认不设置状态，子类可重写
    }

    /**
     * 设置错误状态
     *
     * 子类可重写此方法自定义 Error 状态表现
     *
     * @param msg 错误信息
     */
    protected open fun onError(msg: String) {
        // 默认不设置状态，子类可重写
    }

    /**
     * 发送事件
     *
     * 通过 SharedFlow 发送一次性事件，Activity/Fragment 订阅后自动消费
     *
     * @param event 要发送的事件
     */
    protected fun sendEvent(event: Event) {
        _event.tryEmit(event)
    }

    /**
     * 发送 Toast 事件（占位方法）
     *
     * 子类应重写此方法以定义具体的 Toast 事件类型。
     * 默认实现发送 NoOp 空事件。
     *
     * @param msg 错误信息
     */
    protected open fun onSendToastEvent(msg: String) {
        // 子类重写以发送具体的 ShowToast 事件
        // 例如: sendEvent(LoginEvent.ShowToast(msg))
    }

    /**
     * 异常处理回调
     *
     * 子类可重写此方法自定义异常处理逻辑（如日志上报）
     *
     * @param throwable 异常
     */
    protected open fun onException(throwable: Throwable) {
        // 子类可重写，添加日志上报等逻辑
    }
}
