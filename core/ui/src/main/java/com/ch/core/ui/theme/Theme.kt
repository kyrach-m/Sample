package com.ch.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 应用程序主题组件。
 *
 * 提供 Material 3 主题的封装，支持浅色/深色模式切换，
 * 并通过 CompositionLocal 提供扩展颜色供整个应用使用。
 */

/**
 * CompositionLocal 用于在组合树中传递扩展颜色。
 *
 * 使用 [compositionLocalOf] 创建一个可组合的局部变量，
 * 使得在 AppTheme 内部的任何可组合函数都可以通过
 * [MaterialTheme.extendedColors] 访问扩展颜色。
 */
val LocalExtendedColors = compositionLocalOf { LightExtendedColors }

/**
 * 应用程序主题的可组合函数。
 *
 * 包裹应用的根组件，提供统一的颜色、排版和形状样式。
 * 支持根据系统设置自动切换深色模式，也可以手动指定。
 *
 * @sample
 * ```
 * AppTheme {
 *     // 应用内容
 *     Text("Hello World")
 * }
 * ```
 *
 * @param darkTheme 是否使用深色主题，默认为跟随系统设置
 * @param content 应用的内容组合
 */
@Composable
fun AppTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    // 外部传入 darkTheme 时使用外部值；否则从 ThemeManager 响应式读取
    val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()
    val isDark = darkTheme ?: when (themeMode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

/**
 * MaterialTheme 的扩展属性，用于获取当前主题的扩展颜色。
 *
 * 提供一种简洁的方式在任何可组合函数中访问扩展颜色，
 * 用法与访问 MaterialTheme.colorScheme 类似。
 *
 * @sample
 * ```
 * val colors = MaterialTheme.extendedColors
 * Text(
 *     text = "Success",
 *     color = colors.success,
 * )
 * ```
 *
 * @return 当前主题对应的 [ExtendedColors] 实例
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current
