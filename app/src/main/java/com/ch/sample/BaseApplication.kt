package com.ch.sample

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import com.ch.core.common.logger.Logger
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ch.core.network.client.NetworkClient
import com.ch.core.network.token.TokenManager
import com.ch.core.storage.kv.KVStorage
import com.ch.service.crash.ANRWatchDog
import com.ch.service.crash.CrashHandler
import com.ch.service.crash.LogDumper
import com.ch.service.logger.AnalyticsHelper
import com.ch.service.logger.AppLoggerImpl
import com.ch.service.logger.SampleRateManager
import com.ch.service.logger.tracker.PageTracker
import com.ch.service.logger.worker.AnalyticsUploadWorker
import com.ch.middleware.router.LoginInterceptor
import com.ch.middleware.router.LoginStateProvider
import com.ch.middleware.router.RouterInitializer
import com.ch.sample.startup.ConfigPreloadTask
import com.ch.sample.startup.DatabaseWarmUpTask
import com.ch.service.startup.dag.StartupScheduler
import com.ch.service.startup.idle.IdleTaskExecutor
import com.ch.service.startup.monitor.StartupMonitor
import com.ch.service.startup.battery.BatteryOptimizationHelper
import com.ch.core.network.base.BaseApi
import com.ch.core.network.error.DefaultErrorCodeHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 全局 Application
 *
 * 应用启动入口，负责按严格顺序初始化所有核心组件：
 * 1. KVStorage.init — 最优先，统一 KV 存储（MMKV 多实例）
 * 2. Logger.init — 注入文件日志实现
 * 3. CrashHandler.init — 初始化崩溃捕获
 * 4. NetworkClient.init — 初始化网络模块
 * 5. RouterInitializer.init — 初始化路由模块 + 登录状态提供者
 * 6. StartupScheduler.execute — 执行 DAG 启动任务
 * 7. AnalyticsHelper.init — 事件上报初始化
 * 8. SampleRateManager.init — 采样率管理
 * 9. ANRWatchDog — ANR 监控
 * 10. PageTracker — 页面自动埋点
 * 11. AnalyticsUploadWorker — 埋点定期上传
 * 12. IdleTaskExecutor — 空闲任务执行器
 *
 * 初始化顺序原则：
 * - 存储先于日志（日志可能用到 MMKV）
 * - 日志先于崩溃捕获（崩溃需要日志）
 * - 崩溃捕获先于业务（确保所有异常被捕获）
 * - 核心基础设施先于 DAG 任务调度
 */
class BaseApplication : Application() {

    companion object {
        private const val TAG = "BaseApplication"

        @Volatile
        private var instance: BaseApplication? = null

        fun getInstance(): BaseApplication {
            return instance ?: throw IllegalStateException(
                "BaseApplication 尚未初始化，请检查 AndroidManifest.xml 中是否配置了 android:name"
            )
        }
    }

    /**
     * ANR 监控看门狗
     */
    private lateinit var anrWatchDog: ANRWatchDog

    init {
        instance = this
        StartupMonitor.setProcessStartTime(System.currentTimeMillis())
        StartupMonitor.beginStage(StartupMonitor.Stage.PROCESS_START)
    }

    /**
     * 检查当前是否为主进程
     *
     * @return true=主进程，false=其他进程（如 recovery 进程）
     */
    private fun isMainProcess(): Boolean {
        val packageName = packageName
        val processName = getCurrentProcessName()
        return packageName == processName
    }

    /**
     * 获取当前进程名
     *
     * 使用 ActivityManager 方案兼容所有 API 版本，避免部分 OEM 设备上报的 SDK_INT 不准确导致
     * NoSuchMethodError（如 API 28+ 上报但框架实际缺少 myProcessName 方法）
     */
    private fun getCurrentProcessName(): String {
        val pid = android.os.Process.myPid()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        if (runningProcesses != null) {
            for (process in runningProcesses) {
                if (process.pid == pid) {
                    return process.processName
                }
            }
        }
        return packageName
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        StartupMonitor.endStage(StartupMonitor.Stage.PROCESS_START)
        StartupMonitor.beginStage(StartupMonitor.Stage.APPLICATION_CREATE)

        // ========================================
        // 1. 初始化统一 KV 存储（最优先，日志也需要用它存数据）
        // ========================================
        KVStorage.init(this)

        // ========================================
        // 2. 初始化日志系统（注入文件日志实现）
        // ========================================
        Logger.init(AppLoggerImpl(this, BuildConfig.DEBUG))
        Logger.i(TAG, "日志系统初始化完成 (Debug=${BuildConfig.DEBUG})")

        // ========================================
        // 3. 初始化崩溃捕获
        // ========================================
        CrashHandler.init(this)
        CrashHandler.setLogDumper(LogDumper { maxCount ->
            Logger.dumpRecentLogs(maxCount)
        })
        Logger.i(TAG, "全局异常捕获已注册")

        // ========================================
        // 4. 初始化网络模块
        // ========================================
        // Base URL: https://xxxxxxxxxxxxxxxxxxxxxxxx
        NetworkClient.init(
            this,
            "https://xxxxxxxxxxxxxxxxxxxxxxxx"
        )

        // 配置 Token 刷新回调（必须在使用 Token 之前设置）
        TokenManager.setTokenRefreshCallback {
            refreshToken()
        }

        // 配置 API 错误码处理器
        val errorHandler = DefaultErrorCodeHandler().apply {
            // Token 过期 → 清除本地登录状态并跳转登录页
            onTokenExpired = {
                Logger.w(TAG, "API Token 已过期，需要重新登录")
                TokenManager.clearToken()
                // 可以发送广播通知各模块清理登录状态
            }
            // 账号被踢 → 清除本地登录状态并跳转登录页
            onAccountKicked = {
                Logger.w(TAG, "账号在其他设备登录，被踢下线")
                TokenManager.clearToken()
            }
            // 账号锁定 → 提示用户
            onAccountLocked = { remainingTime ->
                Logger.w(TAG, "账号已被锁定，剩余时间: ${remainingTime}ms")
            }
            // 请求过于频繁 → 提示用户稍后再试
            onRateLimited = {
                Logger.w(TAG, "请求过于频繁，请稍后再试")
            }
            // 服务器错误 → 提示用户稍后再试
            onServerError = {
                Logger.e(TAG, "服务器内部错误")
            }
            // 其他错误 → 统一回调
            onError = { code, msg ->
                Logger.e(TAG, "API 错误码: $code, 消息: $msg")
            }
        }
        BaseApi.setErrorHandler(errorHandler)

        Logger.i(TAG, "网络模块初始化完成")

        // ========================================
        // 5. 初始化路由模块
        // ========================================
        // 设置 App 路由配置器（在 init 之前设置）
        RouterInitializer.setAppRouteConfigurator(AppRouteConfigurator())
        RouterInitializer.init(this)
        // 设置登录状态提供者（供 LoginInterceptor 使用）
        LoginInterceptor.setLoginStateProvider(object : LoginStateProvider {
            override fun isLoggedIn(): Boolean {
                return TokenManager.isLoggedIn()
            }
        })
        // TODO: 创建 features:login 模块后配置登录页路径
        // LoginInterceptor.setLoginPath("/login/LoginActivity")
        Logger.i(TAG, "路由模块初始化完成")

        // ========================================
        // 6. 执行 DAG 启动任务
        // ========================================
        val scheduler = StartupScheduler()
        scheduler.addTask(DatabaseWarmUpTask())
        scheduler.addTask(ConfigPreloadTask())
        scheduler.execute(this)
        Logger.i(TAG, "DAG 启动任务执行完成")

        // ========================================
        // 6.5 电池优化检查（Debug 模式下警告）
        // ========================================
        BatteryOptimizationHelper.checkAndWarn(this, BuildConfig.DEBUG)

        // ========================================
        // 7. 初始化事件上报
        // ========================================
        AnalyticsHelper.init(this)
        Logger.i(TAG, "事件上报助手初始化完成")

        // ========================================
        // 8. 初始化采样率管理
        // ========================================
        SampleRateManager.init()
        Logger.i(TAG, "采样率管理器初始化完成")

        // ========================================
        // 9. 启动 ANR 监控
        // ========================================
        anrWatchDog = ANRWatchDog { anrInfo ->
            Logger.e("ANR", "ANR 检测到:\n${anrInfo.format()}")
            AnalyticsHelper.logEvent("anr_detected", mapOf(
                "stuck_duration" to anrInfo.stuckDuration.toString(),
                "thread" to anrInfo.threadName
            ))
        }
        anrWatchDog.setDebug(BuildConfig.DEBUG)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                anrWatchDog.setAppForeground(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                anrWatchDog.setAppForeground(false)
            }
        })
        anrWatchDog.start()
        Logger.i(TAG, "ANR 监控已启动 (Debug=${BuildConfig.DEBUG})")

        // ========================================
        // 10. 注册页面自动埋点
        // ========================================
        val pageTracker = PageTracker()
        registerActivityLifecycleCallbacks(pageTracker)
        Logger.i(TAG, "页面自动埋点已注册")

        // ========================================
        // 11. 注册埋点定期上传
        // 注意：WorkManager 仅在主进程中初始化，其他进程（如 recovery）跳过
        // ========================================
        if (isMainProcess()) {
            AnalyticsUploadWorker.enqueue(this)
            Logger.i(TAG, "埋点定期上传任务已注册")
        }

        // ========================================
        // 12. 启动空闲任务执行器
        // ========================================
        IdleTaskExecutor.start()

        StartupMonitor.endStage(StartupMonitor.Stage.APPLICATION_CREATE)
    }

    /**
     * 刷新 Access Token
     *
     * 供 TokenManager 回调使用，在 AuthInterceptor 处理 401 时调用。
     * 当前为框架脚手架版本，实际项目中需实现具体的 Token 刷新逻辑。
     *
     * @return true=刷新成功，false=刷新失败
     */
    private suspend fun refreshToken(): Boolean {
        Logger.w(TAG, "refreshToken: 框架脚手架版本，Token 刷新逻辑需在业务模块中实现")
        return false
    }
}
