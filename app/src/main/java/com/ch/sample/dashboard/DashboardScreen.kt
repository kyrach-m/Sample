package com.ch.sample.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ch.core.ui.component.GlobalButton
import com.ch.core.ui.component.GlobalCard
import com.ch.core.ui.theme.ThemeSwitchDialog
import com.ch.core.ui.theme.ThemeManager

/**
 * Dashboard 页面（Compose 版本）
 *
 * 框架能力展示页，直观展示当前框架所有核心组件的运行状态。
 *
 * 功能模块：
 * - App 信息卡片（版本、构建类型、设备信息）
 * - 启动耗时统计
 * - 各组件状态（存储、路由、日志、崩溃捕获、网络）
 * - 功能测试按钮组
 * - 实时日志面板
 *
 * @param viewModel DashboardViewModel 实例
 * @param onNavigateToComponents 跳转到组件库页面回调
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToComponents: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is DashboardEvent.AppendLog -> {
                    logs.add(event.message)
                    if (logs.size > 100) {
                        logs.removeAt(0)
                    }
                    listState.animateScrollToItem(logs.size - 1)
                }
                is DashboardEvent.ShowMessage -> {
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部操作栏
        TopActionBar(
            onThemeClick = { showThemeDialog = true },
            onComponentsClick = onNavigateToComponents
        )

        // App 信息卡片
        AppInfoCard(state = state)

        // 启动耗时卡片
        StartupTimeCard(state = state)

        // 组件状态卡片
        ComponentStatusCard(state = state)

        // 功能测试按钮
        FunctionTestButtons(
            onSimulateCrash = { viewModel.simulateCrash() },
            onClearCache = { viewModel.clearCache() },
            onPrintRoutes = { viewModel.printRoutes() },
            onTestNetwork = { viewModel.testNetwork(context) },
            onClearLogs = { logs.clear() }
        )

        // 日志面板
        LogPanel(
            logs = logs,
            listState = listState,
            modifier = Modifier.weight(1f)
        )
    }

    ThemeSwitchDialog(
        show = showThemeDialog,
        onDismiss = { showThemeDialog = false }
    )
}

/**
 * 顶部操作栏
 */
@Composable
private fun TopActionBar(
    onThemeClick: () -> Unit,
    onComponentsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "框架演示",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onComponentsClick) {
                Text("组件库")
            }
            TextButton(onClick = onThemeClick) {
                Text("切换主题")
            }
        }
    }
}

/**
 * App 信息卡片
 */
@Composable
private fun AppInfoCard(state: DashboardState?) {
    GlobalCard(title = "App 信息") {
        InfoRow(label = "版本号", value = state?.appVersion ?: "-")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "构建类型", value = state?.buildType?.uppercase() ?: "-")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "设备型号", value = state?.deviceId ?: "-")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "屏幕尺寸", value = state?.screenSize ?: "-")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "系统版本", value = state?.systemVersion ?: "-")
    }
}

/**
 * 启动耗时卡片
 */
@Composable
private fun StartupTimeCard(state: DashboardState?) {
    GlobalCard(title = "启动耗时") {
        InfoRow(label = "进程创建", value = "${state?.processCreateTime ?: 0}ms")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "Application", value = "${state?.appCreateTime ?: 0}ms")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "任务执行", value = "${state?.taskExecutionTime ?: 0}ms")
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(
            label = "总计",
            value = "${state?.totalTime ?: 0}ms",
            valueColor = MaterialTheme.colorScheme.primary,
            valueBold = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "内存使用", value = state?.memoryUsage ?: "-")
    }
}

/**
 * 组件状态卡片
 */
@Composable
private fun ComponentStatusCard(state: DashboardState?) {
    GlobalCard(title = "组件状态") {
        StatusRow(
            label = "网络连接",
            status = state?.isNetworkConnected ?: false,
            onlineText = "已连接",
            offlineText = "未连接"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow(
            label = "MMKV 存储",
            status = state?.isStorageReady ?: false,
            onlineText = "就绪",
            offlineText = "未初始化"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow(
            label = "路由系统",
            status = state?.isRouterReady ?: false,
            onlineText = "已注册 ${state?.routeCount ?: 0} 条",
            offlineText = "未初始化"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow(
            label = "日志系统",
            status = state?.isLoggerReady ?: false,
            onlineText = "运行中",
            offlineText = "未初始化"
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusRow(
            label = "崩溃捕获",
            status = state?.isCrashHandlerReady ?: false,
            onlineText = "已启用",
            offlineText = "未启用"
        )
    }
}

/**
 * 功能测试按钮
 */
@Composable
private fun FunctionTestButtons(
    onSimulateCrash: () -> Unit,
    onClearCache: () -> Unit,
    onPrintRoutes: () -> Unit,
    onTestNetwork: () -> Unit,
    onClearLogs: () -> Unit
) {
    GlobalCard(title = "功能测试") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSimulateCrash,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("模拟崩溃", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清除缓存", fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrintRoutes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("打印路由", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onTestNetwork,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("测试网络", fontSize = 12.sp)
                }
            }
            OutlinedButton(
                onClick = onClearLogs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清空日志")
            }
        }
    }
}

/**
 * 日志面板
 */
@Composable
private fun LogPanel(
    logs: List<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    GlobalCard(
        title = "实时日志 (${logs.size} 条)"
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = Color(0xFFE0E0E0),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 状态行
 */
@Composable
private fun StatusRow(
    label: String,
    status: Boolean,
    onlineText: String,
    offlineText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (status) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (status) onlineText else offlineText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
