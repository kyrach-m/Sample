package com.ch.core.base

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.viewbinding.ViewBinding
import com.ch.core.base.event.ViewEvent
import com.ch.core.base.window.WindowSizeClassifier
import com.ch.core.common.util.BuildVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity 基类（商业级增强版）
 *
 * 统一 ViewBinding 初始化、ViewModel 状态观察和事件订阅。
 * 自动收集 State 和 Event，子类只需实现业务逻辑。
 *
 * 用法示例：
 * ```kotlin
 * class MainActivity : BaseActivity<ActivityMainBinding, LoginState, LoginEvent, LoginViewModel>() {
 *
 *     override val viewModel: LoginViewModel by viewModels()
 *
 *     override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
 *         return ActivityMainBinding.inflate(inflater)
 *     }
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.btnLogin.setOnClickListener { viewModel.login("user", "pass") }
 *     }
 *
 *     override fun renderState(state: LoginState) {
 *         binding.tvStatus.text = if (state.isLoggedIn) "已登录" else "未登录"
 *     }
 *
 *     override fun handleEvent(event: LoginEvent) {
 *         when (event) {
 *             is LoginEvent.ShowToast -> Toast.makeText(this, event.msg, Toast.LENGTH_SHORT).show()
 *             else -> {}
 *         }
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 * @param State 页面状态类型
 * @param Event 事件类型
 * @param VM ViewModel 类型
 */
abstract class BaseActivity<
    VB : ViewBinding,
    State,
    Event : ViewEvent,
    VM : BaseViewModel<State, Event>
> : AppCompatActivity() {

    /**
     * 日志标签
     *
     * 默认返回当前类的简单名称，子类可覆写自定义。
     * 用于 Logger 日志输出时标识来源。
     */
    protected val loggerTag: String
        get() = this::class.java.simpleName

    /**
     * ViewBinding 实例
     */
    protected lateinit var binding: VB
        private set

    /**
     * ViewModel 实例
     *
     * 子类通过 by viewModels() 委托初始化
     */
    protected abstract val viewModel: VM

    /**
     * 当前屏幕尺寸类型
     *
     * 响应式布局状态流，在屏幕尺寸变化时（旋转、折叠屏展开/折叠）自动更新。
     * 子类可以收集此 StateFlow 来响应屏幕尺寸变化。
     *
     * 三种类型：
     * - [WindowSizeClassifier.WindowSizeClass.COMPACT]：紧凑型（手机）
     * - [WindowSizeClassifier.WindowSizeClass.MEDIUM]：中型（折叠屏展开、平板竖屏）
     * - [WindowSizeClassifier.WindowSizeClass.EXPANDED]：展开型（平板横屏、桌面模式）
     */
    protected val currentScreenType: StateFlow<WindowSizeClassifier.WindowSizeClass>
        get() = _currentScreenType

    private val _currentScreenType = MutableStateFlow(WindowSizeClassifier.WindowSizeClass.COMPACT)

    /**
     * 提供 ViewBinding 实例
     *
     * @param inflater LayoutInflater
     * @return ViewBinding 实例
     */
    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    /**
     * 初始化 View
     *
     * 在 setContentView 之后调用，用于设置点击事件、初始化列表等
     *
     * @param savedInstanceState 保存的实例状态
     */
    protected abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 渲染页面状态
     *
     * 当 State 变化时回调，用于更新 UI
     *
     * @param state 当前页面状态
     */
    protected abstract fun renderState(state: State)

    /**
     * 处理一次性事件
     *
     * 默认空实现，子类按需覆写处理 Toast、导航等事件
     *
     * @param event 事件
     */
    protected open fun handleEvent(event: Event) {
        // 默认空实现，子类按需覆写
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _currentScreenType.value = WindowSizeClassifier.getWindowSizeClass(this)
        enableEdgeToEdge()
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        initView(savedInstanceState)
        observeViewModel()
    }

    /**
     * 启用 EdgeToEdge 全面屏显示
     *
     * 让内容延伸到状态栏和导航栏下方，实现沉浸式体验。
     * 同时设置状态栏图标颜色自适应（亮色状态栏 → 深色图标，暗色状态栏 → 浅色图标）。
     *
     * 子类如需自定义状态栏样式，可覆写此方法。
     */
    protected open fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = !isDarkTheme()
        }
    }

    /**
     * 判断当前是否为暗色主题
     *
     * 用于决定状态栏图标颜色。默认根据系统主题判断，子类可覆写。
     *
     * @return true=暗色主题，false=亮色主题
     */
    protected open fun isDarkTheme(): Boolean {
        val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 观察 ViewModel 状态和事件变化
     *
     * 在 STARTED 状态下收集，STOPPED 时自动取消
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    updateScreenSize()
                }
                launch {
                    viewModel.state.collectLatest { state ->
                        state?.let { renderState(it) }
                    }
                }
                launch {
                    viewModel.event.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newSize = WindowSizeClassifier.getWindowSizeClass(this)
        if (_currentScreenType.value != newSize) {
            _currentScreenType.value = newSize
        }
    }

    private suspend fun updateScreenSize() {
        val newSize = WindowSizeClassifier.getWindowSizeClass(this@BaseActivity)
        if (_currentScreenType.value != newSize) {
            _currentScreenType.value = newSize
        }
    }
}

/**
 * Fragment 内获取 ViewModel 的扩展函数
 *
 * 简化 by viewModels() 的重复代码
 *
 * 用法示例：
 * ```kotlin
 * class HomeFragment : BaseFragment<FragmentHomeBinding, HomeState, HomeEvent, HomeViewModel>() {
 *     override val viewModel: HomeViewModel by viewModel()
 * }
 * ```
 *
 * @param T ViewModel 类型
 * @return ViewModel 实例
 */
inline fun <reified T : ViewModel> Fragment.viewModel(): Lazy<T> {
    return viewModels()
}

/**
 * Activity 内获取 ViewModel 的扩展函数
 *
 * 简化 by viewModels() 的重复代码
 *
 * 用法示例：
 * ```kotlin
 * class MainActivity : BaseActivity<ActivityMainBinding, State, Event, MainViewModel>() {
 *     override val viewModel: MainViewModel by viewModel()
 * }
 * ```
 *
 * @param T ViewModel 类型
 * @return ViewModel 实例
 */
inline fun <reified T : ViewModel> AppCompatActivity.viewModel(): Lazy<T> {
    return viewModels()
}
