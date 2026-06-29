package com.ch.core.base

import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.widget.FrameLayout
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
import com.ch.core.ui.loading.LoadingMask
import com.ch.core.ui.widget.dialog.GlobalDialog
import com.ch.core.ui.widget.state.GlobalEmptyView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseActivity<
    VB : ViewBinding,
    State,
    Event : ViewEvent,
    VM : BaseViewModel<State, Event>
> : AppCompatActivity() {

    protected val loggerTag: String
        get() = this::class.java.simpleName

    protected lateinit var binding: VB
        private set

    protected abstract val viewModel: VM

    protected val currentScreenType: StateFlow<WindowSizeClassifier.WindowSizeClass>
        get() = _currentScreenType

    private val _currentScreenType = MutableStateFlow(WindowSizeClassifier.WindowSizeClass.COMPACT)

    private var immersiveModeEnabled = false

    // 全局加载遮罩
    private var loadingMask: LoadingMask? = null

    // 全局空状态视图
    private var emptyView: GlobalEmptyView? = null

    // 当前显示的对话框
    private var currentDialog: GlobalDialog? = null

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected abstract fun initView(savedInstanceState: Bundle?)

    protected abstract fun renderState(state: State)

    protected open fun handleEvent(event: Event) {
    }

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

    private fun applyThemeIfNeeded() {
        ThemeManager.isDarkMode(this)
    }

    protected open fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = !ThemeManager.isDarkMode(this)
            controller.isAppearanceLightNavigationBars = !ThemeManager.isDarkMode(this)
        }
    }

    protected fun setStatusBarColor(color: Int, lightIcons: Boolean) {
        window.statusBarColor = color
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = lightIcons
        }
    }

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

    protected fun isImmersiveModeEnabled(): Boolean {
        return immersiveModeEnabled
    }

    protected open fun isDarkTheme(): Boolean {
        return ThemeManager.isDarkMode(this)
    }

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
        // 主题变化时重新应用沉浸式状态栏（更新状态栏图标颜色）
        enableEdgeToEdge()
        // 通知所有 View 重新应用主题属性
        window.decorView.requestLayout()
    }

    override fun onResume() {
        super.onResume()
        fixStatusBarColor()
    }

    protected open fun fixStatusBarColor() {
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = !ThemeManager.isDarkMode(this)
        }
    }

    private suspend fun updateScreenSize() {
        val newSize = WindowSizeClassifier.getWindowSizeClass(this@BaseActivity)
        if (_currentScreenType.value != newSize) {
            _currentScreenType.value = newSize
        }
    }

    // ========================================
    // 全局 UI 方法（业务层零感知调用）
    // ========================================

    /**
     * 显示加载遮罩
     *
     * @param message 加载提示文字
     */
    protected fun showLoading(message: String = "加载中...") {
        if (loadingMask == null) {
            loadingMask = LoadingMask(this)
            val decorView = window.decorView as FrameLayout
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            decorView.addView(loadingMask, lp)
        }
        loadingMask?.show(message)
    }

    /**
     * 显示带进度的加载遮罩
     *
     * @param progress 进度值（0-100）
     * @param message 加载提示文字（可选）
     */
    protected fun showLoadingWithProgress(progress: Int, message: String? = null) {
        if (loadingMask == null) {
            loadingMask = LoadingMask(this)
            val decorView = window.decorView as FrameLayout
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            decorView.addView(loadingMask, lp)
        }
        loadingMask?.showWithProgress(progress, message)
    }

    /**
     * 更新加载进度
     *
     * @param progress 进度值（0-100）
     */
    protected fun updateLoadingProgress(progress: Int) {
        loadingMask?.updateProgress(progress)
    }

    /**
     * 更新加载文案
     *
     * @param message 新的加载提示文字
     */
    protected fun setLoadingMessage(message: String) {
        loadingMask?.setMessage(message)
    }

    /**
     * 隐藏加载遮罩
     */
    protected fun hideLoading() {
        loadingMask?.dismiss()
    }

    /**
     * 显示 Toast
     *
     * @param message 提示文字
     */
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示长 Toast
     *
     * @param message 提示文字
     */
    protected fun showLongToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 显示对话框
     *
     * @param builder GlobalDialog.Builder
     */
    protected fun showDialog(builder: GlobalDialog.Builder) {
        currentDialog?.dismiss()
        currentDialog = builder.show()
    }

    /**
     * 显示确认对话框
     *
     * @param title 标题
     * @param message 消息
     * @param positiveText 确认按钮文字
     * @param onPositive 确认回调
     * @param negativeText 取消按钮文字（可选）
     * @param onNegative 取消回调（可选）
     */
    protected fun showConfirmDialog(
        title: String? = null,
        message: String,
        positiveText: String = "确定",
        onPositive: () -> Unit,
        negativeText: String? = "取消",
        onNegative: (() -> Unit)? = null
    ) {
        currentDialog?.dismiss()
        currentDialog = GlobalDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText, onPositive)
            .let { builder ->
                if (negativeText != null) {
                    builder.setNegativeButton(negativeText, onNegative)
                } else {
                    builder
                }
            }
            .show()
    }

    /**
     * 隐藏对话框
     */
    protected fun hideDialog() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    /**
     * 显示空状态视图
     *
     * @param type 空状态类型
     * @param title 标题（可选）
     * @param subtitle 副标题（可选）
     * @param retryText 重试按钮文字（可选）
     * @param onRetry 重试回调（可选）
     */
    protected fun showEmptyView(
        type: GlobalEmptyView.EmptyType = GlobalEmptyView.EmptyType.EMPTY,
        title: String? = null,
        subtitle: String? = null,
        retryText: String? = null,
        onRetry: (() -> Unit)? = null
    ) {
        if (emptyView == null) {
            emptyView = GlobalEmptyView(this)
            val decorView = window.decorView as FrameLayout
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            lp.gravity = Gravity.CENTER
            decorView.addView(emptyView, lp)
        }
        
        when (type) {
            GlobalEmptyView.EmptyType.EMPTY -> emptyView?.showEmpty(title, subtitle, retryText, onRetry)
            GlobalEmptyView.EmptyType.ERROR -> emptyView?.showError(title, subtitle, retryText, onRetry)
            GlobalEmptyView.EmptyType.NO_RESULT -> emptyView?.showNoResult(title, subtitle)
        }
    }

    /**
     * 隐藏空状态视图
     */
    protected fun hideEmptyView() {
        emptyView?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理全局视图
        loadingMask?.let {
            (window.decorView as FrameLayout).removeView(it)
            loadingMask = null
        }
        emptyView?.let {
            (window.decorView as FrameLayout).removeView(it)
            emptyView = null
        }
        currentDialog?.dismiss()
        currentDialog = null
    }
}

inline fun <reified T : ViewModel> Fragment.viewModel(): Lazy<T> {
    return viewModels()
}

inline fun <reified T : ViewModel> AppCompatActivity.viewModel(): Lazy<T> {
    return viewModels()
}
