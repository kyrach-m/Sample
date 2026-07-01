package com.ch.core.ui.theme

import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ch.core.storage.kv.KVStorage
import com.ch.core.storage.kv.Scope

/**
 * 主题管理器
 *
 * 统一管理应用的深色/浅色主题切换，支持三种模式：
 * - 跟随系统（SYSTEM）：根据系统设置自动切换
 * - 浅色模式（LIGHT）：强制使用浅色主题
 * - 深色模式（DARK）：强制使用深色主题
 *
 * 通过 [StateFlow] 暴露主题状态，Compose UI 自动响应变化，无需重建 Activity。
 * 使用 [KVStorage] 的 CONFIG 作用域持久化存储主题设置。
 *
 * 用法示例：
 * ```kotlin
 * // 设置深色模式
 * ThemeManager.setThemeMode(ThemeMode.DARK)
 *
 * // 观察主题变化（在 Composable 中）
 * val themeMode by ThemeManager.themeMode.collectAsState()
 *
 * // 切换主题
 * ThemeManager.toggleTheme()
 * ```
 */
object ThemeManager {

    /**
     * 主题模式枚举
     */
    enum class ThemeMode {
        /** 跟随系统 */
        SYSTEM,
        /** 浅色模式 */
        LIGHT,
        /** 深色模式 */
        DARK
    }

    private const val KEY_THEME_MODE = "theme_mode"

    /**
     * 当前主题模式（响应式）
     *
     * Compose UI 通过 collectAsState 自动订阅变化，
     * 非 Compose 场景通过 [themeModeValue] 获取当前值。
     */
    private val _themeMode = MutableStateFlow(getPersistedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /**
     * 获取当前主题模式值（非响应式）
     *
     * 适用于非 Compose 场景（如日志、埋点）。
     * Compose UI 应使用 [themeMode] Flow。
     */
    val themeModeValue: ThemeMode
        get() = _themeMode.value

    /**
     * 设置主题模式
     *
     * 设置后立即生效，Compose UI 自动重组，无需重建 Activity。
     *
     * @param mode 主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value != mode) {
            _themeMode.value = mode
            KVStorage.putString(KEY_THEME_MODE, mode.name, Scope.CONFIG)
        }
    }

    /**
     * 判断当前是否为深色主题
     *
     * 根据当前主题模式和系统设置综合判断。
     *
     * @return true 表示深色主题，false 表示浅色主题
     */
    fun isDarkTheme(): Boolean {
        return when (_themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemDarkTheme()
        }
    }

    /**
     * 切换主题
     *
     * 在浅色和深色之间切换。如果当前为 SYSTEM 模式，
     * 则根据当前系统主题切换到相反模式（LIGHT 或 DARK）。
     *
     * @return 切换后的主题模式
     */
    fun toggleTheme(): ThemeMode {
        val newMode = if (isDarkTheme()) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(newMode)
        return newMode
    }

    /**
     * 判断系统是否为深色主题
     */
    private fun isSystemDarkTheme(): Boolean {
        val currentNightMode = android.content.res.Resources.getSystem().configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 从持久化存储中读取主题模式
     */
    private fun getPersistedThemeMode(): ThemeMode {
        val modeName = KVStorage.getString(KEY_THEME_MODE, scope = Scope.CONFIG)
        return if (modeName.isNotEmpty()) {
            try {
                ThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        } else {
            ThemeMode.SYSTEM
        }
    }
}
