package com.ch.sample.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ch.core.ui.component.GlobalDialog
import com.ch.middleware.permission.PermissionDialogState

/**
 * 权限弹窗宿主组件
 *
 * 观察 [PermissionDialogState] 的弹窗请求，自动渲染 [GlobalDialog]。
 * 用户操作后通过 [PermissionDialogState.resolve] 回传结果，
 * PermissionHelper 的挂起协程自动恢复。
 *
 * 应放置在 Activity 的 Compose 内容树顶层，确保所有权限弹窗都能被渲染。
 *
 * 用法：
 * ```kotlin
 * setContent {
 *     AppTheme {
 *         PermissionDialogHost()
 *         ScreenContent()
 *     }
 * }
 * ```
 */
@Composable
fun PermissionDialogHost() {
    val pendingDialog by PermissionDialogState.pendingDialog.collectAsStateWithLifecycle()

    pendingDialog?.let { request ->
        GlobalDialog(
            show = true,
            title = request.title,
            message = request.message,
            confirmText = request.confirmText,
            cancelText = request.cancelText,
            onConfirm = { PermissionDialogState.resolve(true) },
            onDismiss = { PermissionDialogState.resolve(false) }
        )
    }
}
