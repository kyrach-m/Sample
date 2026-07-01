package com.ch.sample.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ch.core.common.logger.Logger
import com.ch.sample.MainActivity
import com.ch.service.startup.dag.StartupScheduler
import com.ch.service.startup.monitor.StartupMonitor

/**
 * 闪屏页 Activity
 *
 * 冷启动入口页面，负责：
 * 1. 消除白屏/黑屏（通过 androidx.core:core-splashscreen 官方库实现）
 * 2. 触发启动任务调度（StartupScheduler.execute）
 * 3. 记录启动耗时（StartupMonitor 埋点）
 * 4. 所有任务完成后平滑过渡到主页
 *
 * 设计说明：
 * - 继承 [ComponentActivity]，纯 Compose 技术栈，不依赖 View 体系
 * - 使用 Theme.SplashScreen 官方主题，系统自动处理自适应图标尺寸适配
 * - 本页面无 Compose UI 内容，闪屏界面完全由系统 SplashScreen 主题渲染
 * - 启动任务完成后直接跳转 MainActivity，过渡动画由 Compose Navigation 处理
 *
 * 启动流程：
 * ```
 * 进程创建 → 系统渲染 SplashScreen（无白屏）
 *     ↓
 * SplashActivity.onCreate() → installSplashScreen()
 *     ↓
 * 启动任务调度执行（StartupScheduler）
 *     ↓
 * 任务完成 → 启动 MainActivity
 *     ↓
 * SplashActivity.finish()
 * ```
 */
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashActivity"

        /**
         * 最小闪屏显示时间（毫秒）
         * 即使启动任务已完成，也至少显示闪屏页这么久，避免闪烁
         */
        private const val MIN_SPLASH_DURATION = 1500L
    }

    /**
     * SplashScreen 实例
     * 用于控制闪屏关闭时机
     */
    private var splashScreen: SplashScreen? = null

    /**
     * 启动任务调度器
     */
    private lateinit var scheduler: StartupScheduler

    /**
     * 闪屏页开始时间
     */
    private var splashStartTime: Long = 0L

    /**
     * 标记是否已准备好关闭闪屏
     *
     * 用于 SplashScreen.setKeepOnScreenCondition 控制闪屏可见性，
     * 确保启动任务完成前闪屏一直显示。
     */
    @Volatile
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // ⚠️ 必须在 super.onCreate() 之前调用，安装官方 SplashScreen
        // 系统会自动处理自适应图标尺寸适配，解决 Logo 放大/闪烁问题
        splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 设置闪屏保持可见条件，直到启动任务完成
        splashScreen?.setKeepOnScreenCondition { !isReady }

        // 记录 Activity 创建时间
        StartupMonitor.endStage(StartupMonitor.Stage.APPLICATION_CREATE)
        StartupMonitor.beginStage(StartupMonitor.Stage.ACTIVITY_CREATE)

        splashStartTime = System.currentTimeMillis()
        Logger.d(TAG, "SplashActivity 创建")

        // 初始化启动调度器
        scheduler = StartupScheduler()
        registerStartupTasks()

        // 执行启动任务
        StartupMonitor.endStage(StartupMonitor.Stage.ACTIVITY_CREATE)
        StartupMonitor.beginStage(StartupMonitor.Stage.TASK_EXECUTION)

        // 在后台线程执行启动任务，避免阻塞主线程
        Thread {
            try {
                scheduler.execute(applicationContext)
            } catch (e: Exception) {
                Logger.e(TAG, "启动任务执行失败", e)
            }

            StartupMonitor.endStage(StartupMonitor.Stage.TASK_EXECUTION)

            // 确保最小闪屏时间
            val elapsed = System.currentTimeMillis() - splashStartTime
            val remaining = MIN_SPLASH_DURATION - elapsed
            if (remaining > 0) {
                Thread.sleep(remaining)
            }

            // 回到主线程跳转到目标页面
            runOnUiThread {
                navigateToDestination()
            }
        }.start()
    }

    /**
     * 注册启动任务
     *
     * 在此方法中添加所有需要通过 DAG 调度器执行的启动任务。
     * 业务层可在此注册自定义的 StartupTask。
     */
    private fun registerStartupTasks() {
        // ========================================
        // TODO: 业务层注册启动任务
        // ========================================
        // 示例：
        // scheduler.addTask(object : StartupTask() {
        //     override val name = "MyTask"
        //     override fun execute(context: Context) {
        //         // 初始化逻辑
        //     }
        //     override fun dependencies() = listOf(NetworkTask::class.java)
        // })
    }

    /**
     * 跳转到目标页面
     *
     * 启动任务完成后直接跳转到主页（框架展示页）。
     *
     * 跳转后 finish() 当前 Activity，避免用户按返回键回到闪屏页。
     */
    private fun navigateToDestination() {
        isReady = true

        Logger.d(TAG, "启动任务完成，跳转到主页")
        navigateToMain()
    }

    /**
     * 跳转到主页
     *
     * 使用 FLAG_ACTIVITY_CLEAR_TASK 确保任务栈干净，
     * SplashScreen 的 fade-out 动画由系统自动处理。
     */
    private fun navigateToMain() {
        StartupMonitor.beginStage(StartupMonitor.Stage.FIRST_FRAME)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()

        StartupMonitor.endStage(StartupMonitor.Stage.FIRST_FRAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 关闭调度器线程池
        if (::scheduler.isInitialized) {
            scheduler.shutdown()
        }
    }
}
