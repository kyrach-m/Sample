package com.ch.service.startup.dag

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ch.core.common.logger.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * DAG 启动任务调度器
 *
 * 基于有向无环图（DAG）的任务调度器，支持：
 * - 自动解析任务依赖关系
 * - 无依赖任务并行执行，有依赖任务等待前置任务完成
 * - 主线程/后台线程自动调度
 * - 超时保护机制
 * - 完整的耗时监控和日志输出
 *
 * 执行流程：
 * 1. 通过 [addTask] 注册所有任务
 * 2. 调用 [execute] 开始调度
 * 3. 内部构建 DAG（[TaskGraph]），检测循环依赖
 * 4. 按分层顺序执行：每层内并行，层间串行
 * 5. 每个任务有超时保护，超时后记录警告并继续
 *
 * 线程模型：
 * - 主线程任务：通过 Handler 发送到主线程执行
 * - 后台任务：使用自定义线程池并行执行
 * - 线程池配置：CPU 核心数 + 1，最大 4 线程
 *
 * 使用示例：
 * ```kotlin
 * val scheduler = StartupScheduler()
 * scheduler.addTask(NetworkTask())
 * scheduler.addTask(StorageTask())
 * scheduler.addTask(UITask())
 * scheduler.execute(context)
 * ```
 *
 * 注意事项：
 * - [execute] 必须在主线程调用
 * - execute 会阻塞当前线程直到所有任务完成（使用 CountDownLatch 同步）
 * - 如需异步执行，请在协程/线程中调用
 */
class StartupScheduler {

    companion object {
        private const val TAG = "StartupScheduler"

        /**
         * 全局默认超时时间（毫秒）
         * 当任务未指定 timeoutMs() 时使用此值
         */
        private const val DEFAULT_TIMEOUT_MS = 5000L

        /**
         * 最大线程数
         */
        private const val MAX_POOL_SIZE = 4
    }

    /**
     * 自定义线程池
     *
     * 配置说明：
     * - 核心线程数：CPU 核心数（保持活跃）
     * - 最大线程数：MAX_POOL_SIZE（防止过多线程竞争）
     * - 空闲存活时间：30 秒
     * - 工作队列：有界队列（容量 64），防止任务堆积
     * - 线程工厂：自定义命名，便于调试
     */
    private val executor: ThreadPoolExecutor by lazy {
        val cpuCount = Runtime.getRuntime().availableProcessors()
        val corePoolSize = cpuCount.coerceAtMost(MAX_POOL_SIZE)

        ThreadPoolExecutor(
            corePoolSize,
            MAX_POOL_SIZE,
            30L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(64),
            StartupThreadFactory(),
            ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用线程执行
        )
    }

    /**
     * 主线程 Handler
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 任务图
     */
    private val taskGraph = TaskGraph()

    /**
     * 是否已执行
     */
    private var isExecuted = false

    /**
     * 任务耗时记录：任务名 → 耗时（毫秒）
     */
    private val taskDurations = mutableMapOf<String, Long>()

    /**
     * 添加启动任务
     *
     * 必须在 [execute] 之前调用。
     *
     * @param task 启动任务实例
     * @throws IllegalStateException 如果已经调用过 [execute]
     */
    fun addTask(task: StartupTask) {
        check(!isExecuted) {
            "已经执行过 execute()，不能继续添加任务"
        }
        taskGraph.addTask(task)
    }

    /**
     * 执行所有已注册的任务
     *
     * 执行流程：
     * 1. 构建 DAG（检测循环依赖）
     * 2. 获取分层执行顺序
     * 3. 逐层执行：每层内并行，层间串行
     * 4. 输出总耗时日志
     *
     * 注意：此方法会阻塞当前线程直到所有任务完成。
     * 如果在主线程调用，主线程任务会直接执行，后台任务通过线程池并行。
     *
     * @param context Application Context
     * @throws CyclicDependencyException 如果检测到循环依赖
     * @throws IllegalStateException 如果没有注册任何任务或已执行过
     */
    fun execute(context: Context) {
        check(!isExecuted) {
            "execute() 只能调用一次"
        }
        isExecuted = true

        val appContext = context.applicationContext
        val totalStart = System.currentTimeMillis()

        Logger.d(TAG, "========================================")
        Logger.d(TAG, "开始执行启动任务调度...")
        Logger.d(TAG, "已注册任务数: ${taskGraph.getAllTasks().size}")

        // 构建 DAG
        taskGraph.build()

        val layers = taskGraph.getExecutionLayers()
        Logger.d(TAG, "DAG 构建完成，共 ${layers.size} 层")
        layers.forEachIndexed { index, layer ->
            Logger.d(TAG, "  第 ${index + 1} 层: ${layer.joinToString(", ")}")
        }

        // 逐层执行
        layers.forEachIndexed { layerIndex, layer ->
            Logger.d(TAG, "--- 执行第 ${layerIndex + 1} 层 ---")
            executeLayer(appContext, layer)
        }

        val totalDuration = System.currentTimeMillis() - totalStart
        Logger.d(TAG, "========================================")
        Logger.d(TAG, "所有启动任务执行完成！总耗时: ${totalDuration}ms")
        Logger.d(TAG, "各任务耗时:")
        taskDurations.forEach { (name, duration) ->
            Logger.d(TAG, "  $name: ${duration}ms")
        }
        Logger.d(TAG, "========================================")
    }

    /**
     * 执行一层任务（层内并行）
     *
     * @param context Application Context
     * @param layer 当前层的任务名列表
     */
    private fun executeLayer(context: Context, layer: List<String>) {
        // 分离主线程任务和后台任务
        val mainThreadTasks = layer.filter { taskGraph.getTask(it)?.runOnMainThread() == true }
        val backgroundTasks = layer.filter { taskGraph.getTask(it)?.runOnMainThread() != true }

        val latch = CountDownLatch(layer.size)

        // 执行后台任务（线程池并行）
        backgroundTasks.forEach { taskName ->
            executor.execute {
                executeTask(context, taskName, latch)
            }
        }

        // 执行主线程任务
        mainThreadTasks.forEach { taskName ->
            executeTask(context, taskName, latch)
        }

        // 等待当前层所有任务完成
        try {
            val maxTimeout = layer.maxOfOrNull { taskName ->
                taskGraph.getTask(taskName)?.timeoutMs() ?: DEFAULT_TIMEOUT_MS
            } ?: DEFAULT_TIMEOUT_MS

            val waitResult = latch.await(maxTimeout * 2, TimeUnit.MILLISECONDS)
            if (!waitResult) {
                Logger.w(TAG, "第层执行超时（${maxTimeout * 2}ms），部分任务可能未完成")
            }
        } catch (e: InterruptedException) {
            Logger.e(TAG, "等待层执行被中断", e)
            Thread.currentThread().interrupt()
        }
    }

    /**
     * 执行单个任务（带超时保护）
     *
     * @param context Application Context
     * @param taskName 任务名
     * @param latch 层同步闩锁
     */
    private fun executeTask(context: Context, taskName: String, latch: CountDownLatch) {
        val task = taskGraph.getTask(taskName)
        if (task == null) {
            Logger.w(TAG, "任务 $taskName 不存在，跳过")
            latch.countDown()
            return
        }

        val startTime = System.currentTimeMillis()
        val timeout = task.timeoutMs()
        var isTimeout = false

        try {
            Logger.d(TAG, "[$taskName] 开始执行 (线程: ${Thread.currentThread().name})")

            if (timeout > 0) {
                // 带超时的执行
                val taskLatch = CountDownLatch(1)
                var exception: Throwable? = null

                val runnable = Runnable {
                    try {
                        task.execute(context)
                    } catch (e: Throwable) {
                        exception = e
                    } finally {
                        taskLatch.countDown()
                    }
                }

                if (task.runOnMainThread()) {
                    mainHandler.post(runnable)
                } else {
                    runnable.run()
                }

                isTimeout = !taskLatch.await(timeout, TimeUnit.MILLISECONDS)

                if (isTimeout) {
                    Logger.w(TAG, "[$taskName] 执行超时 (${timeout}ms)，跳过等待")
                }

                exception?.let {
                    Logger.e(TAG, "[$taskName] 执行异常", it)
                }
            } else {
                // 无超时限制
                task.execute(context)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "[$taskName] 执行失败", e)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            taskDurations[taskName] = duration
            val status = if (isTimeout) "超时" else "完成"
            Logger.d(TAG, "[$taskName] $status 耗时: ${duration}ms")
            latch.countDown()
        }
    }

    /**
     * 获取所有任务的耗时记录
     *
     * @return 任务名 → 耗时（毫秒）的映射
     */
    fun getTaskDurations(): Map<String, Long> = taskDurations.toMap()

    /**
     * 关闭线程池
     *
     * 应在 Application 退出时调用（通常不需要，因为进程会直接结束）。
     * 提供此方法用于测试场景。
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    /**
     * 自定义线程工厂
     *
     * 为线程池中的线程命名，格式：StartupPool-<编号>
     * 便于在日志和调试器中识别线程来源。
     */
    private class StartupThreadFactory : ThreadFactory {
        private val threadNumber = AtomicInteger(1)

        override fun newThread(r: Runnable): Thread {
            return Thread(r, "StartupPool-${threadNumber.getAndIncrement()}").apply {
                isDaemon = false
                priority = Thread.NORM_PRIORITY
            }
        }
    }
}
