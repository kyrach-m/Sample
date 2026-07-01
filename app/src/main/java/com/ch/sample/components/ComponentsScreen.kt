package com.ch.sample.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ch.core.ui.component.GlobalButton
import com.ch.core.ui.component.GlobalCard
import com.ch.core.ui.component.GlobalDialog
import com.ch.core.ui.component.GlobalLoadingMask
import com.ch.core.ui.component.GlobalStateView
import com.ch.core.ui.component.UiState
import com.ch.core.ui.component.ButtonVariant
import com.ch.core.ui.component.ButtonSize

/**
 * 组件库展示页（Compose 版本）
 *
 * 展示 core:ui 模块中所有 Compose 组件的使用示例。
 */
@Composable
fun ComponentsScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf(UiState.SUCCESS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "组件库",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 按钮组件
        GlobalCard(title = "Button 按钮") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlobalButton(
                        text = "主按钮",
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.PRIMARY
                    )
                    GlobalButton(
                        text = "次要",
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.SECONDARY
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlobalButton(
                        text = "文字",
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.TEXT
                    )
                    GlobalButton(
                        text = "危险",
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.DANGER
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlobalButton(
                        text = "大按钮",
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.LARGE
                    )
                    GlobalButton(
                        text = "小按钮",
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.SMALL
                    )
                }
                GlobalButton(
                    text = "加载中",
                    onClick = { },
                    loading = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 对话框组件
        GlobalCard(title = "Dialog 对话框") {
            GlobalButton(
                text = "显示对话框",
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 加载遮罩组件
        GlobalCard(title = "LoadingMask 加载遮罩") {
            GlobalButton(
                text = "显示加载遮罩 (2秒)",
                onClick = { showLoading = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 状态视图组件
        GlobalCard(title = "StateView 状态视图") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlobalButton(
                        text = "加载中",
                        onClick = { uiState = UiState.LOADING },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.SMALL
                    )
                    GlobalButton(
                        text = "成功",
                        onClick = { uiState = UiState.SUCCESS },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.SMALL
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlobalButton(
                        text = "错误",
                        onClick = { uiState = UiState.ERROR },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.SMALL
                    )
                    GlobalButton(
                        text = "空数据",
                        onClick = { uiState = UiState.EMPTY },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.SMALL
                    )
                }
            }
        }
    }

    // 对话框
    GlobalDialog(
        show = showDialog,
        title = "提示",
        message = "这是一个全局对话框示例。",
        onConfirm = { showDialog = false },
        onDismiss = { showDialog = false }
    )

    // 加载遮罩
    if (showLoading) {
        GlobalLoadingMask(
            isLoading = true,
            loadingText = "加载中...",
            content = { }
        )
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showLoading = false
        }
    }
}
