package com.ch.sample

import android.content.Context
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import com.ch.core.base.BaseViewModel
import com.ch.core.base.event.ViewEvent
import com.ch.core.common.logger.Logger
import com.ch.core.common.util.DeviceUtil
import com.ch.core.common.util.NetworkUtil
import com.ch.core.storage.kv.KVStorage
import com.ch.core.storage.kv.Scope
import com.ch.middleware.router.RouterHelper
import com.ch.service.crash.CrashHandler
import com.ch.service.startup.monitor.StartupMonitor
import kotlinx.coroutines.delay

/**
 * Dashboard ViewModel
 *
 * 负责管理 Dashboard 页面的状态和事件。
 */
class DashboardViewModel(
    savedStateHandle: SavedStateHandle
) : BaseViewModel<DashboardState, DashboardEvent>(savedStateHandle) {

    constructor() : this(SavedStateHandle())

    companion object {
        private const val TAG = "DashboardViewModel"
        private const val KEY_DEVICE_ID = "device_id"
    }

    init {
        // 初始化状态
        setState(DashboardState())
    }

    /**
     * 加载 Dashboard 数据
     */
    fun loadDashboardData(context: android.content.Context) {
        launch {
            try {
                // 获取设备ID（如果没有则生成）
                var deviceId = KVStorage.getString(KEY_DEVICE_ID, scope = Scope.CONFIG)
                if (deviceId.isEmpty()) {
                    deviceId = generateDeviceId()
                    KVStorage.putString(KEY_DEVICE_ID, deviceId, Scope.CONFIG)
                }

                // 获取屏幕尺寸
                val screenWidth = DeviceUtil.getScreenWidth(context)
                val screenHeight = DeviceUtil.getScreenHeight(context)
                val screenSize = "${screenWidth} x $screenHeight"

                // 检查各模块状态
                val isNetworkConnected = NetworkUtil.isConnected(context)
                val routeCount = RouterHelper.getRouteCount()
                val isRouterReady = routeCount > 0

                // 获取启动性能数据
                val processCreateTime = StartupMonitor.getStageDuration(StartupMonitor.Stage.PROCESS_START)
                val appCreateTime = StartupMonitor.getStageDuration(StartupMonitor.Stage.APPLICATION_CREATE)
                val taskExecutionTime = StartupMonitor.getStageDuration(StartupMonitor.Stage.TASK_EXECUTION)
                val totalTime = StartupMonitor.getTotalDuration()

                // 获取内存使用情况
                val runtime = Runtime.getRuntime()
                val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val memoryUsage = "${usedMemoryMB}MB"

                setState(
                    DashboardState(
                        appVersion = BuildConfig.VERSION_NAME,
                        buildType = BuildConfig.BUILD_TYPE,
                        deviceId = deviceId,
                        screenSize = screenSize,
                        systemVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        isNetworkConnected = isNetworkConnected,
                        isStorageReady = true,
                        isRouterReady = isRouterReady,
                        routeCount = routeCount,
                        isLoggerReady = true,
                        isCrashHandlerReady = CrashHandler.isInitialized(),
                        logEntries = emptyList(),
                        processCreateTime = processCreateTime,
                        appCreateTime = appCreateTime,
                        taskExecutionTime = taskExecutionTime,
                        totalTime = totalTime,
                        memoryUsage = memoryUsage
                    )
                )

                // 延迟1秒后输出启动日志（打字机效果）
                delay(1000)
                emitStartupLogs()

                sendEvent(DashboardEvent.AppendLog("Dashboard 数据加载完成"))
            } catch (e: Exception) {
                Logger.e(TAG, "加载 Dashboard 数据失败", e)
                sendEvent(DashboardEvent.AppendLog("加载 Dashboard 数据失败: ${e.message}"))
            }
        }
    }

    /**
     * 输出启动日志（打字机效果）
     */
    private suspend fun emitStartupLogs() {
        val startupLogs = listOf(
            "[BaseApplication] Application 构造函数执行",
            "[KVStorage] MMKV 初始化完成",
            "[Logger] 日志系统初始化完成 (Debug=${BuildConfig.DEBUG})",
            "[CrashHandler] 全局异常捕获已注册",
            "[NetworkClient] 网络模块初始化完成",
            "[RouterHelper] 路由表加载完成",
            "[StartupMonitor] 启动阶段监控开始",
            "[StartupMonitor] 进程创建: ${StartupMonitor.getStageDuration(StartupMonitor.Stage.PROCESS_START)}ms",
            "[StartupMonitor] Application创建: ${StartupMonitor.getStageDuration(StartupMonitor.Stage.APPLICATION_CREATE)}ms",
            "[StartupMonitor] 启动任务执行: ${StartupMonitor.getStageDuration(StartupMonitor.Stage.TASK_EXECUTION)}ms",
            "[BaseApplication] onCreate() 执行完成",
            "[SplashActivity] 闪屏页启动",
            "[Router] 跳转至 Dashboard",
            "[Dashboard] 框架能力展示页加载完成"
        )

        startupLogs.forEachIndexed { index, log ->
            delay((50 + index * 20).toLong())
            sendEvent(DashboardEvent.AppendLog(log))
        }
    }

    /**
     * 模拟崩溃测试
     */
    fun simulateCrash() {
        launch {
            sendEvent(DashboardEvent.AppendLog("触发模拟崩溃..."))
            delay(100)
            // 主动抛出 NullPointerException，验证 CrashHandler 是否能捕获
            throw NullPointerException("Dashboard 模拟崩溃测试")
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        launch {
            sendEvent(DashboardEvent.AppendLog("正在清除缓存..."))
            try {
                // 清除 CACHE 作用域的数据
                KVStorage.clear(Scope.CACHE)
                delay(200)
                sendEvent(DashboardEvent.AppendLog("缓存已清除"))
                sendEvent(DashboardEvent.ShowMessage("缓存已清除"))
            } catch (e: Exception) {
                Logger.e(TAG, "清除缓存失败", e)
                sendEvent(DashboardEvent.AppendLog("清除缓存失败: ${e.message}"))
            }
        }
    }

    /**
     * 打印路由表
     */
    fun printRoutes() {
        launch {
            sendEvent(DashboardEvent.AppendLog("正在打印路由表..."))
            try {
                val routes = RouterHelper.printAllRoutes()
                Logger.d(TAG, routes)
                sendEvent(DashboardEvent.AppendLog("路由表已打印到 Logcat"))
                sendEvent(DashboardEvent.ShowMessage("路由表已打印到 Logcat"))
            } catch (e: Exception) {
                Logger.e(TAG, "打印路由表失败", e)
                sendEvent(DashboardEvent.AppendLog("打印路由表失败: ${e.message}"))
            }
        }
    }

    /**
     * 测试网络
     */
    fun testNetwork(context: Context) {
        launch {
            sendEvent(DashboardEvent.AppendLog("正在测试网络..."))
            try {
                val startTime = System.currentTimeMillis()
                val isConnected = NetworkUtil.isConnected(context)
                val endTime = System.currentTimeMillis()
                val elapsed = endTime - startTime

                if (isConnected) {
                    val networkType = if (NetworkUtil.isWifi(context)) "WiFi" else "移动数据"
                    sendEvent(DashboardEvent.AppendLog("网络连接正常 ($networkType, 检测耗时: ${elapsed}ms)"))
                    sendEvent(DashboardEvent.ShowMessage("网络连接正常"))
                } else {
                    sendEvent(DashboardEvent.AppendLog("网络连接失败"))
                    sendEvent(DashboardEvent.ShowMessage("网络连接失败"))
                }
            } catch (e: Exception) {
                Logger.e(TAG, "网络测试失败", e)
                sendEvent(DashboardEvent.AppendLog("网络测试失败: ${e.message}"))
            }
        }
    }

    /**
     * 添加日志条目
     */
    fun addLog(tag: String, message: String) {
        sendEvent(DashboardEvent.AppendLog("[$tag] $message"))
    }

    /**
     * 更新网络状态
     */
    fun updateNetworkStatus(isConnected: Boolean) {
        launch {
            setState(
                state.value?.copy(isNetworkConnected = isConnected) ?: DashboardState(
                    isNetworkConnected = isConnected
                )
            )
            sendEvent(DashboardEvent.AppendLog("网络状态变化: ${if (isConnected) "已连接" else "未连接"}"))
        }
    }

    /**
     * 生成设备ID
     */
    private fun generateDeviceId(): String {
        return "D-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    override fun onException(throwable: Throwable) {
        Logger.e(TAG, "ViewModel 异常: ${throwable.message}", throwable)
        sendEvent(DashboardEvent.AppendLog("错误: ${throwable.message}"))
    }
}

/**
 * Dashboard 页面状态
 */
data class DashboardState(
    val appVersion: String = "",
    val buildType: String = "",
    val deviceId: String = "",
    val screenSize: String = "",
    val systemVersion: String = "",
    val isNetworkConnected: Boolean = false,
    val isStorageReady: Boolean = false,
    val isRouterReady: Boolean = false,
    val routeCount: Int = 0,
    val isLoggerReady: Boolean = false,
    val isCrashHandlerReady: Boolean = false,
    val logEntries: List<String> = emptyList(),
    val processCreateTime: Long = 0L,
    val appCreateTime: Long = 0L,
    val taskExecutionTime: Long = 0L,
    val totalTime: Long = 0L,
    val memoryUsage: String = ""
)

/**
 * Dashboard 事件
 */
sealed class DashboardEvent : ViewEvent {
    data class AppendLog(val message: String) : DashboardEvent()
    data class ShowMessage(val message: String) : DashboardEvent()
}
