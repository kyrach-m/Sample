package com.ch.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全局徽章组件
 *
 * 用于显示数字计数或状态标记的徽章组件，通常与图标、头像等配合使用。
 *
 * 功能特性：
 * - 支持显示数字计数
 * - 支持自定义背景颜色
 * - 自动调整大小以适应内容
 * - 统一的设计风格
 *
 * 用法示例：
 * ```kotlin
 * GlobalBadge(
 *     count = 5,
 *     containerColor = MaterialTheme.colorScheme.error
 * )
 * ```
 *
 * @param count 显示的数字数量，当 count <= 0 时不显示
 * @param containerColor 徽章背景颜色，默认使用主题 error 颜色
 * @param contentColor 徽章内容颜色，默认使用主题 onError 颜色
 * @param maxCount 最大显示数量，超过则显示 maxCount+，默认 99
 * @param modifier 修饰符
 */
@Composable
fun GlobalBadge(
    count: Int = 0,
    containerColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
    maxCount: Int = 99,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    val displayText = if (count > maxCount) "$maxCount+" else count.toString()

    val horizontalPadding = when {
        count < 10 -> 6.dp
        count <= 99 -> 8.dp
        else -> 10.dp
    }

    Box(
        modifier = modifier
            .background(
                color = containerColor,
                shape = CircleShape
            )
            .padding(
                horizontal = horizontalPadding,
                vertical = 2.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
