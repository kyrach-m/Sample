package com.ch.sample.home

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import androidx.lifecycle.SavedStateHandle
import com.ch.core.base.BaseViewModel
import com.ch.core.common.logger.Logger
import com.ch.core.storage.kv.KVStorage
import com.ch.core.storage.kv.Scope
import com.ch.service.startup.monitor.StartupMonitor
import com.ch.middleware.router.RouterInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

/**
 * Dashboard ViewModel
 *
 * 框架展示页的 ViewModel，负责管理 Dashboard 页面的状态和业务逻辑。
 * 提供各种框架组件的状态展示和测试功能。
 */
class DashboardViewModel(
    savedStateHandle: SavedStateHandle = SavedStateHandle()
) : BaseViewModel<DashboardState, DashboardEvent>(savedStateHandle) {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    init {
        setState(DashboardState())
    }

    /**
     * 加载 Dashboard 数据
     *
     * 初始化所有框架组件的状态信息。
     *
     * @param context 上下文
     */
    fun loadDashboardData(context: Context) {
        launch {
            // IO 操作切到后台线程，避免阻塞主线程导致白屏
            val data = withContext(Dispatchers.IO) {
                val appVersion = getAppVersion(context)
                val buildType = getBuildType(context)
                val deviceId = getDeviceId()
                val screenSize = getScreenSize(context)
                val systemVersion = getSystemVersion()
                val memoryUsage = getMemoryUsage()
                val isStorageReady = checkStorageReady()
                val isRouterReady = checkRouterReady()
                val routeCount = getRouteCount()
                val isLoggerReady = checkLoggerReady()
                val isCrashHandlerReady = checkCrashHandlerReady()

                DashboardState(
                    appVersion = appVersion,
                    buildType = buildType,
                    deviceId = deviceId,
                    screenSize = screenSize,
                    systemVersion = systemVersion,
                    processCreateTime = StartupMonitor.getStageDuration(StartupMonitor.Stage.PROCESS_START),
                    appCreateTime = StartupMonitor.getStageDuration(StartupMonitor.Stage.APPLICATION_CREATE),
                    taskExecutionTime = StartupMonitor.getStageDuration(StartupMonitor.Stage.TASK_EXECUTION),
                    totalTime = StartupMonitor.getTotalDuration(),
                    memoryUsage = memoryUsage,
                    isStorageReady = isStorageReady,
                    isRouterReady = isRouterReady,
                    routeCount = routeCount,
                    isLoggerReady = isLoggerReady,
                    isCrashHandlerReady = isCrashHandlerReady
                )
            }

            setState(data)

            addLog(TAG, "Dashboard 数据加载完成")
            addLog(TAG, "App 版本: ${data.appVersion} (${data.buildType})")
            addLog(TAG, "设备: ${data.deviceId} | 系统: ${data.systemVersion}")
            addLog(TAG, "存储: ${if (data.isStorageReady) "就绪" else "未初始化"}")
            addLog(TAG, "路由: ${data.routeCount} 条已注册")
        }
    }

    /**
     * 更新网络状态
     *
     * @param isConnected 是否已连接
     */
    fun updateNetworkStatus(isConnected: Boolean) {
        val currentState = state.value ?: DashboardState()
        setState(currentState.copy(isNetworkConnected = isConnected))
    }

    /**
     * 模拟崩溃
     *
     * 用于测试全局崩溃捕获功能。
     */
    fun simulateCrash() {
        launch {
            addLog(TAG, "模拟崩溃触发...")
            throw RuntimeException("测试崩溃 - DashboardSimulateCrash")
        }
    }

    /**
     * 清除缓存
     *
     * 清除 CACHE 作用域的所有 KV 存储数据。
     */
    fun clearCache() {
        launch {
            try {
                KVStorage.clear(Scope.CACHE)
                addLog(TAG, "缓存已清除")
                sendEvent(DashboardEvent.ShowMessage("缓存清除成功"))
            } catch (e: Exception) {
                addLog(TAG, "缓存清除失败: ${e.message}")
                sendEvent(DashboardEvent.ShowMessage("缓存清除失败"))
            }
        }
    }

    /**
     * 打印路由表
     *
     * 打印所有已注册的路由信息。
     */
    fun printRoutes() {
        launch {
            try {
                val routes = RouterInitializer.getAllRoutes()
                addLog(TAG, "路由表 (共 ${routes.size} 条):")
                routes.forEachIndexed { index, route ->
                    addLog(TAG, "  ${index + 1}. $route")
                }
                sendEvent(DashboardEvent.ShowMessage("已打印 ${routes.size} 条路由"))
            } catch (e: Exception) {
                addLog(TAG, "打印路由失败: ${e.message}")
            }
        }
    }

    /**
     * 测试网络
     *
     * 测试网络连接状态。
     *
     * @param context 上下文
     */
    fun testNetwork(context: Context) {
        launch {
            addLog(TAG, "开始网络测试...")
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val network = cm.activeNetwork
                val capabilities = network?.let { cm.getNetworkCapabilities(it) }
                val isConnected = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                val networkType = when {
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动数据"
                    else -> "未知"
                }

                addLog(TAG, "网络状态: ${if (isConnected) "已连接 ($networkType)" else "未连接"}")
                updateNetworkStatus(isConnected)
                sendEvent(DashboardEvent.ShowMessage(if (isConnected) "网络正常" else "网络未连接"))
            } catch (e: Exception) {
                addLog(TAG, "网络测试失败: ${e.message}")
            }
        }
    }

    /**
     * 添加日志
     *
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun addLog(tag: String, message: String) {
        Logger.d(tag, message)
        sendEvent(DashboardEvent.AppendLog("[$tag] $message"))
    }

    // ==================== 私有方法 ====================

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    private fun getBuildType(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val isDebug = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebug) "debug" else "release"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    private fun getDeviceId(): String {
        return Build.DEVICE
    }

    private fun getScreenSize(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        val heightDp = displayMetrics.heightPixels / displayMetrics.density
        return "${widthDp.toInt()}x${heightDp.toInt()}dp"
    }

    private fun getSystemVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val percent = (usedMem * 100 / maxMem).toInt()
        val df = DecimalFormat("#.##")
        return "${df.format(usedMem / 1024.0 / 1024.0)}MB / ${df.format(maxMem / 1024.0 / 1024.0)}MB ($percent%)"
    }

    private fun checkStorageReady(): Boolean {
        return try {
            KVStorage.getString("__test__", "default", Scope.CONFIG)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRouterReady(): Boolean {
        return try {
            RouterInitializer.isInitialized()
        } catch (e: Exception) {
            false
        }
    }

    private fun getRouteCount(): Int {
        return try {
            RouterInitializer.getAllRoutes().size
        } catch (e: Exception) {
            0
        }
    }

    private fun checkLoggerReady(): Boolean {
        return try {
            Logger.d(TAG, "Logger 检查")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkCrashHandlerReady(): Boolean {
        return try {
            Class.forName("com.ch.service.crash.CrashHandler")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    override fun onSendToastEvent(msg: String) {
        sendEvent(DashboardEvent.ShowMessage(msg))
    }
}
