package com.ch.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全局列表项组件
 *
 * 基于 Material 3 设计规范的列表项组件，支持标题、辅助文本、
 * 前导图标和尾部组件。
 *
 * 功能特性：
 * - 支持标题和辅助文本
 * - 支持前导图标
 * - 支持尾部组件
 * - 支持点击事件
 * - 统一的设计风格和内边距
 *
 * 用法示例：
 * ```kotlin
 * GlobalListItem(
 *     headline = "列表项标题",
 *     supportingText = "辅助描述文字",
 *     leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
 *     trailing = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
 *     onClick = { /* 处理点击 */ }
 * )
 * ```
 *
 * @param headline 主标题文字
 * @param supportingText 辅助描述文字，可选
 * @param leadingIcon 前导图标，可选
 * @param trailing 尾部组件，可选
 * @param onClick 点击回调，为 null 时不可点击
 * @param modifier 修饰符
 */
@Composable
fun GlobalListItem(
    headline: String,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(16.dp))
        }

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.padding(top = 2.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailing()
        }
    }
}
