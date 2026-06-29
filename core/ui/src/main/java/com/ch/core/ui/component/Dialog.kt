package com.ch.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全局对话框组件
 *
 * 基于 Material 3 AlertDialog 封装的统一对话框组件，
 * 提供确认、取消等常见操作按钮。
 *
 * 功能特性：
 * - 支持标题和消息内容
 * - 支持自定义确认和取消按钮文字
 * - 支持确认和取消回调
 * - 统一的设计风格
 *
 * 用法示例：
 * ```kotlin
 * GlobalDialog(
 *     show = showDialog,
 *     title = "提示",
 *     message = "确定要删除吗？",
 *     confirmText = "确定",
 *     cancelText = "取消",
 *     onConfirm = { /* 确认操作 */ },
 *     onDismiss = { showDialog = false }
 * )
 * ```
 *
 * @param show 是否显示对话框
 * @param title 对话框标题
 * @param message 对话框消息内容
 * @param confirmText 确认按钮文字，默认"确定"
 * @param cancelText 取消按钮文字，默认"取消"
 * @param onConfirm 确认按钮点击回调
 * @param onDismiss 取消/关闭对话框回调
 * @param modifier 修饰符
 */
@Composable
fun GlobalDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String = "确定",
    cancelText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = confirmText,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = cancelText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
