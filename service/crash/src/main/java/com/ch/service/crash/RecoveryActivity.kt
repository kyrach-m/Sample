package com.ch.service.crash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ch.core.common.util.AppUtil
import com.ch.core.common.logger.Logger
import kotlinx.coroutines.delay

/**
 * 崩溃恢复页面（Compose 实现）
 *
 * 功能：
 * - 展示崩溃摘要信息（错误类型、错误消息）
 * - 提供"重启应用"和"反馈问题"按钮
 * - 60 秒无操作自动结束进程
 *
 * 设计说明：
 * - 使用 Compose 渲染，与项目纯 Compose 技术栈保持一致
 * - 运行在独立进程（:recovery），不依赖主进程状态
 * - 使用 Material3 自定义配色，不依赖系统主题
 */
class RecoveryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorSummary = intent.getStringExtra(EXTRA_ERROR_SUMMARY) ?: "未知错误"

        setContent {
            val colorScheme = lightColorScheme(
                primary = Color(0xFF1976D2),
                onPrimary = Color.White,
                secondary = Color(0xFF6B7280),
                onSecondary = Color.White,
                background = Color(0xFFF9FAFB),
                onBackground = Color(0xFF111827),
                surface = Color.White,
                onSurface = Color(0xFF111827),
                error = Color(0xFFDC2626),
                errorContainer = Color(0xFFFEE2E2),
                onSurfaceVariant = Color(0xFF6B7280)
            )

            MaterialTheme(colorScheme = colorScheme) {
                RecoveryScreen(
                    errorSummary = errorSummary,
                    onRestart = { restartApp() },
                    onFeedback = { openFeedback(errorSummary) },
                    onTimeout = { finish() }
                )
            }
        }
    }

    private fun restartApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "重启应用失败: ${e.message}")
        }
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun openFeedback(errorSummary: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "应用崩溃反馈")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "=== 崩溃信息 ===\n$errorSummary\n\n=== 用户描述 ===\n（请在此处描述操作步骤）"
                )
            }
            startActivity(Intent.createChooser(intent, "选择反馈方式"))
        } catch (e: Exception) {
            Logger.e(TAG, "打开反馈失败: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RecoveryActivity"
        const val EXTRA_ERROR_SUMMARY = "extra_error_summary"
    }
}

// ─────────────────────────────────────────────
// Compose UI
// ─────────────────────────────────────────────

@Composable
private fun RecoveryScreen(
    errorSummary: String,
    onRestart: () -> Unit,
    onFeedback: () -> Unit,
    onTimeout: () -> Unit
) {
    val context = LocalContext.current
    val parsedFields = remember { parseErrorSummary(errorSummary) }
    val appName = remember {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }
    val appVersion = remember { AppUtil.getVersionName(context) }

    // 60秒无操作自动结束
    LaunchedEffect(Unit) {
        delay(60_000L)
        onTimeout()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 警告图标（圆形背景 + 感叹号，避免依赖 Material Icons Extended）
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "应用遇到了问题",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "我们已记录了错误信息，您可以重启应用或反馈问题",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 崩溃信息卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "应用", value = appName)
                    InfoRow(label = "版本", value = appVersion)
                    parsedFields["type"]?.let { InfoRow(label = "错误类型", value = it) }
                    parsedFields["message"]?.let { InfoRow(label = "错误消息", value = it) }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "重启应用",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "反馈问题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$appName v$appVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp
        )
    }
}

/**
 * 解析错误摘要
 *
 * 格式：`ExceptionType: message`
 */
private fun parseErrorSummary(summary: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val colonIndex = summary.indexOf(": ")
    if (colonIndex > 0) {
        result["type"] = summary.substring(0, colonIndex)
        result["message"] = summary.substring(colonIndex + 2)
    } else {
        result["message"] = summary
    }
    return result
}
