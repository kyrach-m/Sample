package com.ch.core.base

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ch.core.base.event.ViewEvent
import com.ch.core.base.window.WindowSizeClassifier
import com.ch.core.common.util.BuildVersion
import com.ch.core.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * XML + ViewBinding 体系的 Activity 基类
 *
 * ## 定位与职责
 * 为所有使用 **XML 布局 + ViewBinding** 的页面提供统一的基础能力，包括：
 * - ViewBinding 自动初始化与生命周期管理
 * - ViewModel 状态（State）/ 事件（Event）自动订阅
 * - 沉浸式状态栏（Edge-to-Edge）
 * - 主题切换监听与状态栏图标颜色自适应
 * - 屏幕尺寸分类（适配折叠屏 / 平板）
 *
 * ## 继承体系
 * ```
 * AppCompatActivity
 *   └── BaseActivity<VB, State, Event, VM>   ← 本类（XML 布局）
 * ComponentActivity
 *   └── BaseComposeActivity<State, Event, VM> ← Compose 布局专用
 * ```
 *
 * ## 与 BaseComposeActivity 的区别
 *
 * | 对比项 | BaseActivity（本类） | BaseComposeActivity |
 * |--------|----------------------|---------------------|
 * | 父类 | AppCompatActivity | ComponentActivity |
 * | UI 体系 | XML 布局 + ViewBinding | Jetpack Compose 声明式 UI |
 * | 泛型参数 | 4 个（VB, State, Event, VM） | 3 个（State, Event, VM），无 ViewBinding |
 * | 布局方式 | [inflateBinding] 返回 ViewBinding | [ScreenContent] 返回 @Composable |
 * | 状态观察 | 自动 collect → [renderState] | 手动调用 [collectState] 在 Composable 内收集 |
 * | 事件观察 | 自动 collect → [handleEvent] | 自动 collect → [handleEvent] |
 * | 提示反馈 | [showToast] / [showLongToast] | [showSnackbar]（通过 SnackbarHostState） |
 * | 主题应用 | [ThemeManager.isDarkMode] 同步模式 | [ThemeManager.applyTheme] 异步模式 |
 * | 适用场景 | 传统 View 页面、需要兼容旧代码的页面 | 纯 Compose 新页面 |
 *
 * ## 使用示例
 * ```kotlin
 * class LoginActivity : BaseActivity<
 *     ActivityLoginBinding,
 *     LoginState,
 *     LoginEvent,
 *     LoginViewModel
 * >() {
 *
 *     override val viewModel: LoginViewModel by viewModel()
 *
 *     override fun inflateBinding(inflater: LayoutInflater) =
 *         ActivityLoginBinding.inflate(inflater)
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.btnLogin.setOnClickListener { viewModel.login() }
 *     }
 *
 *     override fun renderState(state: LoginState) {
 *         binding.tvStatus.text = state.statusText
 *     }
 *
 *     override fun handleEvent(event: LoginEvent) {
 *         when (event) {
 *             is LoginEvent.ShowToast -> showToast(event.msg)
 *             is LoginEvent.NavigateToHome -> navigateToHome()
 *         }
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型，对应页面的 XML 布局绑定类
 * @param State 页面 UI 状态类型，通常为 data class，由 ViewModel 通过 StateFlow 驱动
 * @param Event 一次性事件类型（Toast、导航、弹窗等），由 ViewModel 通过 SharedFlow 发送
 * @param VM ViewModel 类型，必须继承 [BaseViewModel]
 *
 * @see BaseComposeActivity Compose 体系的 Activity 基类
 * @see BaseFragment Fragment 基类（同样支持 XML + ViewBinding 体系）
 * @see BaseViewModel ViewModel 基类（提供 State/Event 管理）
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
     * 默认返回当前类的简单名称（如 "LoginActivity"）。
     * 用于 Logger 日志输出时标识来源，子类可覆写自定义。
     */
    protected val loggerTag: String
        get() = this::class.java.simpleName

    /**
     * ViewBinding 实例
     *
     * 在 [onCreate] 中通过 [inflateBinding] 初始化，整个 Activity 生命周期内有效。
     * 子类通过此属性访问布局中的 View 控件。
     */
    protected lateinit var binding: VB
        private set

    /**
     * ViewModel 实例
     *
     * 子类必须通过 `by viewModel()` 委托提供实例：
     * ```kotlin
     * override val viewModel: MyViewModel by viewModel()
     * ```
     */
    protected abstract val viewModel: VM

    /**
     * 当前屏幕尺寸分类
     *
     * 响应式布局使用，在屏幕旋转、折叠屏展开/折叠时自动更新。
     * 子类可在 [initView] 中 collect 此 Flow 以响应屏幕尺寸变化。
     *
     * @see WindowSizeClassifier
     */
    protected val currentScreenType: StateFlow<WindowSizeClassifier.WindowSizeClass>
        get() = _currentScreenType

    /** 内部可变屏幕尺寸流 */
    private val _currentScreenType = MutableStateFlow(WindowSizeClassifier.WindowSizeClass.COMPACT)

    /** 当前是否处于沉浸式模式（全屏隐藏导航栏） */
    private var immersiveModeEnabled = false

    /**
     * 创建 ViewBinding 实例
     *
     * 在 [onCreate] 中调用，子类实现此方法返回对应布局的 ViewBinding。
     *
     * @param inflater LayoutInflater 用于 inflate 布局
     * @return ViewBinding 实例
     */
    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    /**
     * 初始化 View
     *
     * 在 [onCreate] 的 setContentView 之后调用，用于：
     * - 设置点击事件
     * - 初始化列表适配器
     * - 设置 Toolbar 等
     *
     * @param savedInstanceState 保存的实例状态，首次创建时为 null
     */
    protected abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 渲染页面状态
     *
     * 当 ViewModel 的 State 发生变化时自动回调。子类在此方法中更新 UI：
     * - 更新 TextView 文本
     * - 显示/隐藏 View
     * - 更新列表数据等
     *
     * @param state 当前最新的 UI 状态
     */
    protected abstract fun renderState(state: State)

    /**
     * 处理一次性事件
     *
     * 当 ViewModel 通过 SharedFlow 发送事件时回调。用于处理：
     * - 显示 Toast
     * - 页面导航
     * - 显示弹窗等一次性操作
     *
     * 默认空实现，子类按需覆写。
     *
     * @param event 事件对象
     */
    protected open fun handleEvent(event: Event) {
    }

    /**
     * Activity 创建入口
     *
     * 执行顺序：
     * 1. [applyThemeIfNeeded] — 在 super.onCreate 前应用主题（避免白屏闪烁）
     * 2. super.onCreate — Activity 标准创建流程
     * 3. 初始化屏幕尺寸分类
     * 4. [enableEdgeToEdge] — 开启沉浸式状态栏
     * 5. [inflateBinding] + setContentView — 设置布局
     * 6. [initView] — 子类初始化 View
     * 7. [observeViewModel] — 开始观察 ViewModel 状态和事件
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeIfNeeded()
        super.onCreate(savedInstanceState)
        _currentScreenType.value = WindowSizeClassifier.getWindowSizeClass(this)
        enableEdgeToEdge()
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        initView(savedInstanceState)
        observeViewModel()
    }

    /**
     * 在 super.onCreate 前应用主题
     *
     * 必须在 setContentView 之前调用，否则主题切换时可能出现白屏闪烁。
     * 调用 [ThemeManager.isDarkMode] 触发主题初始化。
     */
    private fun applyThemeIfNeeded() {
        ThemeManager.isDarkMode(this)
    }

    /**
     * 开启 Edge-to-Edge 沉浸式状态栏
     *
     * 设置内容延伸到系统栏下方，并根据当前主题自动调整状态栏/导航栏图标颜色：
     * - 浅色主题：深色图标（isAppearanceLightStatusBars = true）
     * - 深色主题：浅色图标（isAppearanceLightStatusBars = false）
     *
     * 子类可覆写此方法自定义沉浸式行为。
     */
    protected open fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = !ThemeManager.isDarkMode(this)
            controller.isAppearanceLightNavigationBars = !ThemeManager.isDarkMode(this)
        }
    }

    /**
     * 设置状态栏颜色
     *
     * 用于需要自定义状态栏背景色的特殊页面（如启动页、视频播放页）。
     *
     * @param color 状态栏背景颜色
     * @param lightIcons true = 深色图标（浅色背景），false = 浅色图标（深色背景）
     */
    protected fun setStatusBarColor(color: Int, lightIcons: Boolean) {
        window.statusBarColor = color
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = lightIcons
        }
    }

    /**
     * 设置沉浸式模式（全屏隐藏系统栏）
     *
     * 用于视频播放、图片浏览等需要全屏展示的场景。
     * 退出沉浸式时会自动恢复 Edge-to-Edge 状态。
     *
     * @param enable true = 进入沉浸式，false = 退出沉浸式
     */
    protected fun setImmersiveMode(enable: Boolean) {
        immersiveModeEnabled = enable
        val decorView = window.decorView
        if (enable) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            enableEdgeToEdge()
        }
    }

    /**
     * 查询当前是否处于沉浸式模式
     *
     * @return true = 沉浸式模式已开启
     */
    protected fun isImmersiveModeEnabled(): Boolean {
        return immersiveModeEnabled
    }

    /**
     * 查询当前是否为深色主题
     *
     * 子类可覆写此方法自定义主题判断逻辑。
     *
     * @return true = 深色主题
     */
    protected open fun isDarkTheme(): Boolean {
        return ThemeManager.isDarkMode(this)
    }

    /**
     * 开始观察 ViewModel 的 State 和 Event
     *
     * 在 [Lifecycle.State.STARTED] 状态下收集 Flow，确保：
     * - Activity 不可见时不消费事件，避免后台弹窗等问题
     * - 回到前台时自动恢复收集
     *
     * 内部启动三个协程：
     * 1. 屏幕尺寸更新
     * 2. State 收集 → [renderState]
     * 3. Event 收集 → [handleEvent]
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

    /**
     * 配置变化回调（屏幕旋转、深色模式切换、语言切换等）
     *
     * 处理逻辑：
     * 1. 重新计算屏幕尺寸分类，更新 [currentScreenType]
     * 2. 重新应用沉浸式状态栏图标颜色（适配新主题）
     * 3. 触发 View 重新布局（应用新主题属性）
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newSize = WindowSizeClassifier.getWindowSizeClass(this)
        if (_currentScreenType.value != newSize) {
            _currentScreenType.value = newSize
        }
        // 主题变化时重新应用沉浸式状态栏（更新状态栏图标颜色）
        enableEdgeToEdge()
        // 通知所有 View 重新应用主题属性
        window.decorView.requestLayout()
    }

    /**
     * onResume 时修复状态栏图标颜色
     *
     * 某些系统 ROM 在 Activity 切换时可能错误地重置状态栏图标颜色，
     * 此处确保每次回到前台时图标颜色与当前主题一致。
     */
    override fun onResume() {
        super.onResume()
        fixStatusBarColor()
    }

    /**
     * 修复状态栏图标颜色
     *
     * 子类可覆写此方法自定义修复逻辑。
     */
    protected open fun fixStatusBarColor() {
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = !ThemeManager.isDarkMode(this)
        }
    }

    /**
     * 更新屏幕尺寸分类
     *
     * 在 observeViewModel 中作为独立协程运行，
     * 当屏幕尺寸发生变化时更新 [currentScreenType]。
     */
    private suspend fun updateScreenSize() {
        val newSize = WindowSizeClassifier.getWindowSizeClass(this@BaseActivity)
        if (_currentScreenType.value != newSize) {
            _currentScreenType.value = newSize
        }
    }

    /**
     * 显示短时 Toast
     *
     * @param message 提示文字
     */
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示长时 Toast
     *
     * @param message 提示文字
     */
    protected fun showLongToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

/**
 * Fragment 内获取 ViewModel 的便捷扩展
 *
 * 等价于 `by viewModels()`，提供更语义化的调用方式。
 * 在 [BaseFragment] 中推荐使用此方法：
 * ```kotlin
 * override val viewModel: MyViewModel by viewModel()
 * ```
 *
 * @param T ViewModel 类型
 * @return Lazy 委托实例
 */
inline fun <reified T : ViewModel> Fragment.viewModel(): Lazy<T> {
    return viewModels()
}

/**
 * AppCompatActivity 内获取 ViewModel 的便捷扩展
 *
 * 等价于 `by viewModels()`，提供更语义化的调用方式。
 *
 * @param T ViewModel 类型
 * @return Lazy 委托实例
 */
inline fun <reified T : ViewModel> AppCompatActivity.viewModel(): Lazy<T> {
    return viewModels()
}
