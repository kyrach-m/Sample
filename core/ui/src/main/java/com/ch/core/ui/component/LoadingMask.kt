package com.ch.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 全局加载遮罩组件
 *
 * 在内容上方显示半透明遮罩和加载指示器，用于页面加载、
 * 数据提交等场景，阻止用户操作。
 *
 * 功能特性：
 * - 半透明黑色背景遮罩
 * - 中央显示圆形加载指示器
 * - 支持自定义加载提示文字
 * - 加载时拦截所有用户操作
 *
 * 用法示例：
 * ```kotlin
 * GlobalLoadingMask(
 *     isLoading = isLoading,
 *     loadingText = "加载中..."
 * ) {
 *     // 页面内容
 * }
 * ```
 *
 * @param isLoading 是否显示加载遮罩
 * @param loadingText 加载提示文字，可选
 * @param modifier 修饰符
 * @param content 被遮罩覆盖的内容
 */
@Composable
fun GlobalLoadingMask(
    isLoading: Boolean,
    loadingText: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )

                    if (loadingText != null) {
                        Text(
                            text = loadingText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
