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

    override fun onDestroy() {
        super.onDestroy()
    }
}

inline fun <reified T : ViewModel> Fragment.viewModel(): Lazy<T> {
    return viewModels()
}

inline fun <reified T : ViewModel> AppCompatActivity.viewModel(): Lazy<T> {
    return viewModels()
}
