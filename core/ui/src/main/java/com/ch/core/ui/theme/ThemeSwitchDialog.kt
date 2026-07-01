package com.ch.core.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ch.core.ui.theme.ThemeManager
import com.ch.core.ui.theme.ThemeManager.ThemeMode

/**
 * 主题切换对话框（Compose 版本）
 *
 * 提供浅色/深色/跟随系统三种主题模式的选择界面，
 * 使用 Material 3 风格的对话框设计。
 *
 * 用法示例：
 * ```kotlin
 * var showThemeDialog by remember { mutableStateOf(false) }
 *
 * ThemeSwitchDialog(
 *     show = showThemeDialog,
 *     onDismiss = { showThemeDialog = false }
 * )
 * ```
 *
 * @param show 是否显示对话框
 * @param onDismiss 关闭对话框回调
 * @param onThemeChanged 主题切换完成回调
 */
@Composable
fun ThemeSwitchDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onThemeChanged: ((ThemeMode) -> Unit)? = null
) {
    if (show) {
        var selectedMode by remember { mutableStateOf(ThemeManager.getThemeMode()) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "主题设置",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOption(
                        mode = ThemeMode.LIGHT,
                        selected = selectedMode == ThemeMode.LIGHT,
                        onClick = { selectedMode = ThemeMode.LIGHT }
                    )
                    ThemeOption(
                        mode = ThemeMode.DARK,
                        selected = selectedMode == ThemeMode.DARK,
                        onClick = { selectedMode = ThemeMode.DARK }
                    )
                    ThemeOption(
                        mode = ThemeMode.SYSTEM,
                        selected = selectedMode == ThemeMode.SYSTEM,
                        onClick = { selectedMode = ThemeMode.SYSTEM }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ThemeManager.setThemeMode(selectedMode)
                    onThemeChanged?.invoke(selectedMode)
                    onDismiss()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 主题选项卡片
 *
 * @param mode 主题模式
 * @param selected 是否选中
 * @param onClick 点击回调
 */
@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeIcon(mode = mode)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getThemeName(mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getThemeDescription(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

/**
 * 主题图标
 *
 * @param mode 主题模式
 */
@Composable
private fun ThemeIcon(mode: ThemeMode) {
    val iconRes = when (mode) {
        ThemeMode.LIGHT -> android.R.drawable.ic_menu_view
        ThemeMode.DARK -> android.R.drawable.ic_menu_close_clear_cancel
        ThemeMode.SYSTEM -> android.R.drawable.ic_menu_help
    }
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = getThemeName(mode),
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

/**
 * 获取主题名称
 *
 * @param mode 主题模式
 * @return 主题显示名称
 */
private fun getThemeName(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> "浅色模式"
        ThemeMode.DARK -> "深色模式"
        ThemeMode.SYSTEM -> "跟随系统"
    }
}

/**
 * 获取主题描述
 *
 * @param mode 主题模式
 * @return 主题描述文本
 */
private fun getThemeDescription(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> "明亮清爽，适合日间使用"
        ThemeMode.DARK -> "暗色护眼，适合夜间使用"
        ThemeMode.SYSTEM -> "根据系统设置自动切换"
    }
}
