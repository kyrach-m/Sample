package com.ch.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * UI 状态枚举
 *
 * 定义页面或组件的常见加载状态，用于统一管理不同状态下的 UI 展示。
 */
enum class UiState {
    /** 加载中状态 */
    LOADING,

    /** 加载成功状态 */
    SUCCESS,

    /** 加载失败状态 */
    ERROR,

    /** 空数据状态 */
    EMPTY
}

/**
 * 全局状态视图组件
 *
 * 根据 UiState 自动切换显示不同的状态视图，包括加载中、空数据、
 * 错误重试和成功内容。
 *
 * 功能特性：
 * - 支持 4 种状态：LOADING、SUCCESS、ERROR、EMPTY
 * - 加载状态显示圆形进度条
 * - 错误状态显示错误信息和重试按钮
 * - 空状态显示空数据提示
 * - 成功状态显示自定义内容
 *
 * 用法示例：
 * ```kotlin
 * GlobalStateView(
 *     state = uiState,
 *     onRetry = { viewModel.refresh() },
 *     modifier = Modifier.fillMaxSize()
 * ) {
 *     // 成功时的内容
 *     LazyColumn { ... }
 * }
 * ```
 *
 * @param state 当前 UI 状态
 * @param onRetry 重试按钮点击回调，用于 ERROR 状态
 * @param modifier 修饰符
 * @param loadingText 加载状态提示文字，默认"加载中..."
 * @param errorText 错误状态提示文字，默认"加载失败，请重试"
 * @param emptyText 空状态提示文字，默认"暂无数据"
 * @param retryText 重试按钮文字，默认"重试"
 * @param content 成功状态下显示的内容
 */
@Composable
fun GlobalStateView(
    state: UiState,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loadingText: String = "加载中...",
    errorText: String = "加载失败，请重试",
    emptyText: String = "暂无数据",
    retryText: String = "重试",
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        when (state) {
            UiState.LOADING -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            UiState.SUCCESS -> {
                content()
            }

            UiState.ERROR -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = onRetry) {
                            Text(text = retryText)
                        }
                    }
                }
            }

            UiState.EMPTY -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
