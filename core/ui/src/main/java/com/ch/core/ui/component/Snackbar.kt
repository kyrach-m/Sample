package com.ch.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 全局 Snackbar 组件
 *
 * 基于 Material 3 [SnackbarHost] 封装的统一轻提示组件，
 * 用于替代直接使用原生 SnackbarHost 的场景。
 *
 * 与 [GlobalDialog]（强弹窗）互补，适用于：
 * - 操作成功/失败的轻量反馈
 * - 信息提示（不阻断用户操作）
 * - 配合 [BaseComposeActivity.showSnackbar] 使用
 *
 * 用法示例：
 * ```kotlin
 * // 在 Activity.setContent 中
 * val snackbarHostState = remember { SnackbarHostState() }
 *
 * GlobalSnackbarHost(
 *     hostState = snackbarHostState,
 *     modifier = Modifier.fillMaxSize()
 * )
 *
 * // 显示消息
 * snackbarHostState.showSnackbar("操作成功")
 * ```
 *
 * @param hostState Snackbar 宿主状态，控制消息的显示与队列
 * @param modifier 修饰符
 */
@Composable
fun GlobalSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.inversePrimary,
            )
        }
    )
}
