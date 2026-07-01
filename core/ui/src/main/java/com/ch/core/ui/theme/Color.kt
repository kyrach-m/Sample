package com.ch.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 应用程序的颜色定义。
 *
 * 包含 Material 3 颜色方案的浅色和深色版本，以及一组扩展颜色。
 */

/**
 * 浅色模式颜色方案。
 *
 * 主色调使用紫色系，辅以中性色和表面色，
 * 提供清晰的视觉层次和良好的可读性。
 */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Color(0xFF6750A4),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color.Black,
)

/**
 * 深色模式颜色方案。
 *
 * 针对低光环境优化的颜色方案，使用深色背景和柔和的前景色，
 * 减少眼睛疲劳并提供沉浸式体验。
 */
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFFD0BCFF),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black,
)

/**
 * 扩展颜色集合。
 *
 * 提供 Material 3 标准颜色方案之外的语义化颜色，
 * 用于表示成功、警告、错误、信息等状态。
 *
 * @property success 成功状态颜色，绿色
 * @property warning 警告状态颜色，橙色
 * @property error 错误状态颜色，红色
 * @property info 信息状态颜色，蓝色
 */
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
)

/**
 * 浅色模式的扩展颜色。
 */
val LightExtendedColors = ExtendedColors(
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFF9800),
    error = Color(0xFFB3261E),
    info = Color(0xFF2196F3),
)

/**
 * 深色模式的扩展颜色。
 */
val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF81C784),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFF2B8B5),
    info = Color(0xFF64B5F6),
)
