package com.ch.service.crash

import android.os.Handler
import android.os.Looper
import com.ch.core.common.logger.Logger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * ANR 监控看门狗
 *
 * 通过 WatchDog 模式检测主线程卡顿（ANR）。
 * 原理：后台线程每隔 1 秒向主线程 post 一个 Runnable，
 * 如果主线程在指定时间内未执行该 Runnable，则判定为 ANR。
 *
 * 核心特性：
 * - 每 1 秒检测一次主线程响应
 * - 连续 5 秒无响应判定为 ANR
 * - ANR 发生时记录主线程堆栈
 * - 支持前后台感知（后台时暂停检测，防止误报）
 * - Debug 模式下自动关闭（避免断点导致误报）
 * - 通过回调上报 ANR 信息
 *
 * 使用示例：
 * ```kotlin
 * val watchdog = ANRWatchDog { anrInfo ->
 *     // 上报 ANR 信息
 *     Logger.e("ANR", "ANR 检测到:\n${anrInfo.stackTrace}")
 * }
 * watchdog.start()
 *
 * // 前后台切换时
 * watchdog.setAppForeground(true)  // 前台：恢复检测
 * watchdog.setAppForeground(false) // 后台：暂停检测
 *
 * // 停止
 * watchdog.stop()
 * ```
 *
 * 注意事项：
 * - Debug 模式下（isDebug = true）不会启动检测，避免断点误报
 * - 应用进入后台时应调用 [setAppForeground](false) 暂停检测
 * - 应用回到前台时调用 [setAppForeground](true) 恢复检测
 * - 系统休眠导致的误报通过前后台感知来避免
 */
class ANRWatchDog(
    /**
     * ANR 检测回调
     *
     * @param info ANR 信息，包含堆栈和卡顿时长
     */
    private val onANR: (ANRInfo) -> Unit
) {

    companion object {
        private const val TAG = "ANRWatchDog"

        /**
         * 检测间隔（毫秒）
         */
        private const val CHECK_INTERVAL_MS = 1000L

        /**
         * ANR 判定阈值（毫秒）
         * 主线程超过此时间未响应，判定为 ANR
         */
        private const val ANR_THRESHOLD_MS = 5000L
    }

    /**
     * 主线程响应标记
     *
     * WatchDog 线程 post Runnable 到主线程后，
     * 主线程执行 Runnable 会将此标记置为 true。
     * WatchDog 线程检查此标记判断主线程是否存活。
     */
    private val responded = AtomicBoolean(true)

    /**
     * 连续未响应计数
     *
     * 每次检测如果主线程未响应，计数器 +1。
     * 达到阈值后判定为 ANR。
     */
    private val missCount = AtomicLong(0)

    /**
     * 是否正在运行
     */
    @Volatile
    private var isRunning = false

    /**
     * 是否处于前台
     */
    @Volatile
    private var isForeground = true

    /**
     * 是否 Debug 模式
     */
    @Volatile
    private var isDebug = false

    /**
     * WatchDog 线程
     */
    private var watchThread: Thread? = null

    /**
     * 主线程 Handler
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 设置 Debug 模式
     *
     * Debug 模式下不会启动检测（避免断点导致误报）。
     *
     * @param debug 是否为 Debug 模式
     */
    fun setDebug(debug: Boolean) {
        isDebug = debug
    }

    /**
     * 设置应用前后台状态
     *
     * 后台时暂停检测，防止系统休眠导致误报。
     *
     * @param foreground true=前台（恢复检测），false=后台（暂停检测）
     */
    fun setAppForeground(foreground: Boolean) {
        isForeground = foreground
        if (foreground) {
            // 回到前台时重置状态
            responded.set(true)
            missCount.set(0)
        }
        Logger.d(TAG, "应用前后台切换: foreground=$foreground")
    }

    /**
     * 启动 ANR 监控
     *
     * 启动 WatchDog 线程，每隔 [CHECK_INTERVAL_MS] 检测主线程响应。
     *
     * 注意：
     * - Debug 模式下不会启动
     * - 重复调用会忽略
     */
    fun start() {
        if (isDebug) {
            Logger.d(TAG, "Debug 模式下 ANR WatchDog 不启动（避免断点误报）")
            return
        }
        if (isRunning) {
            Logger.w(TAG, "ANR WatchDog 已经在运行")
            return
        }

        isRunning = true
        watchThread = Thread({
            Logger.d(TAG, "ANR WatchDog 启动")
            while (isRunning) {
                try {
                    Thread.sleep(CHECK_INTERVAL_MS)

                    // 后台时跳过检测
                    if (!isForeground) {
                        responded.set(true)
                        missCount.set(0)
                        continue
                    }

                    // 重置标记
                    responded.set(false)

                    // 向主线程 post 心跳检测
                    mainHandler.post {
                        responded.set(true)
                    }

                    // 等待一个检测周期
                    Thread.sleep(CHECK_INTERVAL_MS)

                    // 检查主线程是否响应
                    if (!responded.get()) {
                        val count = missCount.incrementAndGet()
                        val stuckDuration = count * CHECK_INTERVAL_MS

                        Logger.w(TAG, "主线程未响应 ($count 次，累计 ${stuckDuration}ms)")

                        // 达到 ANR 阈值
                        if (stuckDuration >= ANR_THRESHOLD_MS) {
                            reportANR(stuckDuration)
                            missCount.set(0)
                        }
                    } else {
                        // 主线程正常响应，重置计数
                        missCount.set(0)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Logger.e(TAG, "WatchDog 线程异常", e)
                }
            }
            Logger.d(TAG, "ANR WatchDog 已停止")
        }, "ANR-WatchDog").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * 停止 ANR 监控
     */
    fun stop() {
        isRunning = false
        watchThread?.interrupt()
        watchThread = null
        Logger.d(TAG, "ANR WatchDog 停止")
    }

    /**
     * 上报 ANR 信息
     *
     * 收集主线程堆栈并回调给业务层。
     *
     * @param stuckDuration 卡顿时长（毫秒）
     */
    private fun reportANR(stuckDuration: Long) {
        try {
            // 获取主线程堆栈
            val mainThread = Looper.getMainLooper().thread
            val stackTrace = mainThread.stackTrace

            val stackString = stackTrace.joinToString("\n") { element ->
                "    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
            }

            val anrInfo = ANRInfo(
                timestamp = System.currentTimeMillis(),
                stuckDuration = stuckDuration,
                stackTrace = stackString,
                threadName = mainThread.name
            )

            Logger.e(TAG, "ANR 检测到！卡顿 ${stuckDuration}ms\n$stackString")

            // 回调给业务层
            onANR(anrInfo)
        } catch (e: Exception) {
            Logger.e(TAG, "ANR 信息收集失败", e)
        }
    }

    /**
     * ANR 信息数据类
     *
     * @property timestamp ANR 发生时间戳
     * @property stuckDuration 卡顿时长（毫秒）
     * @property stackTrace 主线程堆栈
     * @property threadName 主线程名称
     */
    data class ANRInfo(
        val timestamp: Long,
        val stuckDuration: Long,
        val stackTrace: String,
        val threadName: String
    ) {
        /**
         * 格式化为可读字符串
         */
        fun format(): String {
            return buildString {
                appendLine("═══════ ANR 检测 ═══════")
                appendLine("时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))}")
                appendLine("卡顿: ${stuckDuration}ms")
                appendLine("线程: $threadName")
                appendLine("堆栈:")
                appendLine(stackTrace)
                appendLine("═══════════════════════")
            }
        }
    }
}
