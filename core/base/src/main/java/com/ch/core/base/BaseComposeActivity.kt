package com.ch.core.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ch.core.base.event.ViewEvent
import com.ch.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Compose Activity 基类
 *
 * 提供基于 Jetpack Compose 的 Activity 基类，继承自 ComponentActivity。
 * 内置沉浸式状态栏、主题管理、ViewModel 绑定、状态/事件观察等功能。
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
     * 内容是否准备就绪
     *
     * 控制 Compose 内容是否从占位状态切换到真实页面。
     * 使用 mutableStateOf 确保修改后触发重组。
     */
    private var isContentReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEvents()

        setContent {
            AppTheme {
                // 1. 占位层：品牌色背景 + 品牌文字，避免白屏
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isContentReady) {
                        // 显示品牌文字，让用户感知正在加载
                        Text(
                            text = "Sample",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. 等待 Compose 运行时完全初始化后标记就绪
                LaunchedEffect(Unit) {
                    // 让出当前协程，确保 Compose 完成首次组合
                    yield()
                    // 标记内容就绪，触发重组显示真实页面
                    isContentReady = true
                }

                // 3. 内容就绪后，显示真实页面
                if (isContentReady) {
                    ScreenContent()
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
     * 获取当前状态的 StateFlow 扩展
     *
     * 在 Composable 中使用此方法收集状态，自动绑定生命周期。
     */
    @Composable
    protected fun collectState(): State? {
        return viewModel.state.collectAsStateWithLifecycle().value
    }
}
