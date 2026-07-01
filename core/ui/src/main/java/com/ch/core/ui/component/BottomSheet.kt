package com.ch.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全局底部面板组件
 *
 * 基于 Material 3 ModalBottomSheet 封装的统一底部面板组件，
 * 支持从底部滑出的模态面板。
 *
 * 功能特性：
 * - 支持显示/隐藏控制
 * - 支持滑动关闭
 * - 支持自定义内容
 * - 统一的设计风格
 *
 * 用法示例：
 * ```kotlin
 * GlobalBottomSheet(
 *     show = showBottomSheet,
 *     onDismiss = { showBottomSheet = false }
 * ) {
 *     Text("底部面板内容")
 * }
 * ```
 *
 * @param show 是否显示底部面板
 * @param onDismiss 面板关闭回调
 * @param modifier 修饰符
 * @param skipPartiallyExpanded 是否跳过部分展开状态，默认 true
 * @param content 面板内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    LaunchedEffect(show) {
        if (show) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (show) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = sheetState,
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.1f)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        ) {
            content()
        }
    }
}
