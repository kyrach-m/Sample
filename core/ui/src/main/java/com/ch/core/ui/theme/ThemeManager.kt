package com.ch.core.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
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
 * 使用 [KVStorage] 的 CONFIG 作用域持久化存储主题设置。
 *
 * 用法示例：
 * ```kotlin
 * // 设置深色模式
 * ThemeManager.setThemeMode(ThemeMode.DARK)
 *
 * // 获取当前是否为深色主题
 * val isDark = ThemeManager.isDarkTheme()
 *
 * // 应用主题（在 Activity.onCreate 中调用）
 * ThemeManager.applyTheme(activity)
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
     * 获取当前主题模式
     *
     * @return 当前主题模式，默认为跟随系统
     */
    fun getThemeMode(): ThemeMode {
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

    /**
     * 设置主题模式
     *
     * 设置后立即生效并持久化存储。
     *
     * @param mode 主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        KVStorage.putString(KEY_THEME_MODE, mode.name, Scope.CONFIG)
        applyNightMode(mode)
    }

    /**
     * 判断当前是否为深色主题
     *
     * 根据当前主题模式和系统设置综合判断。
     *
     * @return true 表示深色主题，false 表示浅色主题
     */
    fun isDarkTheme(): Boolean {
        return when (getThemeMode()) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemDarkTheme()
        }
    }

    /**
     * 判断当前是否为深色主题（兼容旧版 API）
     *
     * @param context 上下文（未使用，保持 API 兼容）
     * @return true 表示深色主题，false 表示浅色主题
     */
    fun isDarkMode(context: Context? = null): Boolean {
        return isDarkTheme()
    }

    /**
     * 应用主题到 Activity
     *
     * 在 Activity 的 super.onCreate() 之前调用，
     * 确保主题正确应用。
     *
     * @param context Activity 上下文
     */
    fun applyTheme(context: Context) {
        applyNightMode(getThemeMode())
    }

    /**
     * 切换主题
     *
     * 在浅色和深色之间切换，系统模式下切换为浅色。
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
     *
     * @return true 表示系统当前为深色主题
     */
    private fun isSystemDarkTheme(): Boolean {
        val currentNightMode = android.content.res.Resources.getSystem().configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 应用夜间模式
     *
     * 使用 AppCompatDelegate 设置全局夜间模式。
     *
     * @param mode 主题模式
     */
    private fun applyNightMode(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
