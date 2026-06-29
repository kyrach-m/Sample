package com.ch.core.base

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ch.core.base.event.ViewEvent
import com.ch.core.ui.theme.AppTheme
import com.ch.core.ui.theme.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Compose Activity 基类
 *
 * 提供基于 Jetpack Compose 的 Activity 基类，继承自 ComponentActivity。
 * 内置沉浸式状态栏、主题管理、ViewModel 绑定、状态/事件观察等功能。
 *
 * 与 [BaseActivity] 的区别：
 * - 使用 Compose 声明式 UI 替代 XML + ViewBinding
 * - 使用 [AppTheme] 提供主题支持
 * - 使用 [enableEdgeToEdge] 实现沉浸式状态栏
 *
 * 用法示例：
 * ```kotlin
 * class DashboardActivity : BaseComposeActivity<DashboardState, DashboardEvent, DashboardViewModel>() {
 *     override val viewModel: DashboardViewModel by viewModels()
 *
 *     @Composable
 *     override fun ScreenContent() {
 *         DashboardScreen()
 *     }
 * }
 * ```
 *
 * @param State 页面状态类型
 * @param Event 事件类型，必须是 ViewEvent 的子类
 * @param VM ViewModel 类型，必须继承自 BaseViewModel
 */
abstract class BaseComposeActivity<
    State,
    Event : ViewEvent,
    VM : BaseViewModel<State, Event>
> : ComponentActivity() {

    /**
     * 日志标签
     */
    protected val loggerTag: String
        get() = this::class.java.simpleName

    /**
     * ViewModel 实例
     *
     * 子类通过 by viewModels() 委托提供实例。
     */
    protected abstract val viewModel: VM

    /**
     * Snackbar 宿主状态
     *
     * 用于在 Compose 中显示 Snackbar 提示。
     */
    protected val snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeIfNeeded()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEvents()
        setContent {
            AppTheme(darkTheme = ThemeManager.isDarkTheme()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenContent()
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }

    /**
     * 屏幕内容
     *
     * 子类实现此方法提供页面的 Compose 内容。
     * 已经包裹在 [AppTheme] 中，可以直接使用 MaterialTheme 访问主题。
     */
    @Composable
    protected abstract fun ScreenContent()

    /**
     * 观察 ViewModel 事件
     *
     * 在生命周期 STARTED 状态下收集事件流，调用 [handleEvent] 处理。
     */
    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collectLatest { event ->
                    handleEvent(event)
                }
            }
        }
    }

    /**
     * 处理 ViewModel 事件
     *
     * 子类可重写此方法处理自定义事件。
     * 默认实现为空操作。
     *
     * @param event 事件对象
     */
    protected open fun handleEvent(event: Event) {
    }

    /**
     * 应用主题
     *
     * 在 super.onCreate() 之前调用，确保主题正确应用。
     * 根据 [ThemeManager] 的设置决定使用浅色还是深色主题。
     */
    private fun applyThemeIfNeeded() {
        ThemeManager.applyTheme(this)
    }

    /**
     * 显示 Snackbar
     *
     * 在 Compose 页面中显示短时提示信息。
     *
     * @param message 提示消息
     */
    protected fun showSnackbar(message: String) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.decorView.requestLayout()
    }

    /**
     * 获取当前状态的 StateFlow 扩展
     *
     * 在 Composable 中使用此方法收集状态，自动绑定生命周期。
     */
    @Composable
    protected fun collectState(): State? {
        return viewModel.state.collectAsStateWithLifecycle().value
    }
}
