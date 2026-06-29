package com.ch.service.startup.monitor

import android.os.Looper
import com.ch.core.common.logger.Logger

/**
 * 启动全链路耗时监控
 *
 * 记录 App 冷启动各阶段的耗时，用于性能分析和优化。
 * 所有阶段完成后，输出完整的启动耗时报告。
 *
 * 启动阶段定义：
 * 1. [Stage.PROCESS_START] — 进程创建（Application 构造前）
 * 2. [Stage.APPLICATION_CREATE] — Application.onCreate() 执行
 * 3. [Stage.TASK_EXECUTION] — 启动任务调度执行
 * 4. [Stage.ACTIVITY_CREATE] — SplashActivity.onCreate() 执行
 * 5. [Stage.FIRST_FRAME] — 首帧渲染完成
 *
 * 使用方式：
 * ```kotlin
 * // 在 Application 构造中
 * StartupMonitor.beginStage(StartupMonitor.Stage.PROCESS_START)
 *
 * // 在 Application.onCreate() 中
 * StartupMonitor.endStage(StartupMonitor.Stage.PROCESS_START)
 * StartupMonitor.beginStage(StartupMonitor.Stage.APPLICATION_CREATE)
 *
 * // 在 SplashActivity 中
 * StartupMonitor.endStage(StartupMonitor.Stage.APPLICATION_CREATE)
 * StartupMonitor.beginStage(StartupMonitor.Stage.ACTIVITY_CREATE)
 * ```
 *
 * 线程安全：
 * - 所有方法均使用同步块保护
 * - beginStage/endStage 应在主线程调用
 * - getReport() 可在任意线程调用
 *
 * 注意：
 * - 每个阶段必须先 beginStage 再 endStage
 * - 重复 beginStage 会覆盖之前的开始时间
 * - 未 endStage 的阶段不会出现在报告中
 */
object StartupMonitor {

    private const val TAG = "StartupMonitor"

    /**
     * 启动阶段枚举
     *
     * 定义冷启动的 5 个关键阶段，按时间顺序排列。
     */
    enum class Stage(val description: String) {
        /** 进程创建阶段 */
        PROCESS_START("进程创建"),
        /** Application.onCreate() 阶段 */
        APPLICATION_CREATE("Application 创建"),
        /** 启动任务调度执行阶段 */
        TASK_EXECUTION("启动任务执行"),
        /** Activity 创建阶段 */
        ACTIVITY_CREATE("Activity 创建"),
        /** 首帧渲染阶段 */
        FIRST_FRAME("首帧渲染")
    }

    /**
     * 各阶段的开始时间
     */
    private val stageStartTimes = mutableMapOf<Stage, Long>()

    /**
     * 各阶段的耗时记录
     */
    private val stageDurations = mutableMapOf<Stage, Long>()

    /**
     * 整体启动时间戳（进程创建时间）
     */
    private var processStartTime: Long = 0L

    /**
     * 是否已完成所有阶段
     */
    @Volatile
    private var isComplete = false

    /**
     * 记录进程启动时间
     *
     * 应在 Application 构造方法中尽早调用。
     * 如果无法获取精确的进程启动时间，可使用 System.currentTimeMillis()。
     *
     * @param timeMs 进程启动时间戳（毫秒）
     */
    fun setProcessStartTime(timeMs: Long) {
        synchronized(this) {
            processStartTime = timeMs
        }
    }

    /**
     * 开始记录某个阶段
     *
     * 记录当前时间作为阶段的开始时间。
     * 如果阶段已开始但未结束，再次调用会覆盖开始时间。
     *
     * @param stage 启动阶段
     */
    fun beginStage(stage: Stage) {
        synchronized(this) {
            stageStartTimes[stage] = System.currentTimeMillis()
            Logger.d(TAG, "▶ 开始阶段: ${stage.description}")
        }
    }

    /**
     * 结束记录某个阶段
     *
     * 计算从 [beginStage] 到当前的耗时。
     * 如果未调用 beginStage，则忽略。
     *
     * @param stage 启动阶段
     */
    fun endStage(stage: Stage) {
        synchronized(this) {
            val startTime = stageStartTimes[stage]
            if (startTime == null) {
                Logger.w(TAG, "⚠ 阶段 ${stage.description} 未调用 beginStage，忽略 endStage")
                return
            }

            val duration = System.currentTimeMillis() - startTime
            stageDurations[stage] = duration
            Logger.d(TAG, "◀ 结束阶段: ${stage.description} 耗时: ${duration}ms")

            // 检查是否所有阶段都已完成
            if (stageDurations.size == Stage.entries.size) {
                isComplete = true
                printReport()
            }
        }
    }

    /**
     * 获取指定阶段的耗时
     *
     * @param stage 启动阶段
     * @return 耗时（毫秒），阶段未完成时返回 -1
     */
    fun getStageDuration(stage: Stage): Long {
        synchronized(this) {
            return stageDurations[stage] ?: -1L
        }
    }

    /**
     * 获取总启动耗时
     *
     * 从进程启动到首帧渲染的总时间。
     *
     * @return 总耗时（毫秒），未完成时返回 -1
     */
    fun getTotalDuration(): Long {
        synchronized(this) {
            if (!isComplete || processStartTime == 0L) return -1L
            return stageDurations.values.sum()
        }
    }

    /**
     * 获取启动耗时报告
     *
     * @return 格式化的耗时报告字符串
     */
    fun getReport(): String {
        synchronized(this) {
            val sb = StringBuilder()
            sb.appendLine("╔══════════════════════════════════════╗")
            sb.appendLine("║       启动全链路耗时报告              ║")
            sb.appendLine("╠══════════════════════════════════════╣")

            var totalDuration = 0L
            Stage.entries.forEach { stage ->
                val duration = stageDurations[stage]
                val durationStr = if (duration != null) {
                    totalDuration += duration
                    "${duration}ms"
                } else {
                    "未完成"
                }
                sb.appendLine("║ ${stage.description.padEnd(14)} $durationStr")
            }

            sb.appendLine("╠══════════════════════════════════════╣")
            sb.appendLine("║ 总耗时: ${totalDuration}ms")
            sb.appendLine("╚══════════════════════════════════════╝")

            return sb.toString()
        }
    }

    /**
     * 输出耗时报告到日志
     */
    private fun printReport() {
        Logger.d(TAG, getReport())
        Logger.d(TAG, "启动监控完成，所有阶段耗时已记录")
    }

    /**
     * 重置监控数据
     *
     * 仅用于测试场景，正常使用时不应调用。
     */
    fun reset() {
        synchronized(this) {
            stageStartTimes.clear()
            stageDurations.clear()
            processStartTime = 0L
            isComplete = false
        }
    }
}
