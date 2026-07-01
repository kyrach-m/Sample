package com.ch.core.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 主题切换对话框
 *
 * 展示三种主题模式（跟随系统 / 浅色 / 深色），
 * 用户选择后立即生效，通过 [ThemeManager] 持久化。
 *
 * @param show 是否显示对话框
 * @param onDismiss 关闭回调
 */
@Composable
fun ThemeSwitchDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val currentMode by ThemeManager.themeMode.collectAsStateWithLifecycle()

    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "切换主题",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(Modifier.selectableGroup()) {
                    for (mode in ThemeManager.ThemeMode.values()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = mode == currentMode,
                                    onClick = {
                                        ThemeManager.setThemeMode(mode)
                                        onDismiss()
                                    }
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == currentMode,
                                onClick = {
                                    ThemeManager.setThemeMode(mode)
                                    onDismiss()
                                }
                            )
                            Text(
                                text = when (mode) {
                                    ThemeManager.ThemeMode.SYSTEM -> "跟随系统"
                                    ThemeManager.ThemeMode.LIGHT -> "浅色模式"
                                    ThemeManager.ThemeMode.DARK -> "深色模式"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
