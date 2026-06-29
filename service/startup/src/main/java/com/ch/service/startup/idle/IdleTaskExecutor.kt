package com.ch.service.startup.idle

import android.os.Looper
import android.os.MessageQueue
import com.ch.core.common.logger.Logger
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 空闲任务执行器
 *
 * 利用主线程空闲时间执行非关键路径的初始化任务。
 * 通过 [Looper.myQueue().addIdleHandler] 监听主线程空闲状态，
 * 在主线程无 UI 渲染压力时执行延迟任务。
 *
 * 核心特性：
 * - 按优先级排序执行（高优先级先执行）
 * - 支持一次性任务和周期性任务
 * - 自动在主线程空闲时触发
 * - 线程安全，可在任意线程注册任务
 *
 * 适用场景：
 * - 广告 SDK 初始化
 * - 推送 SDK 注册
 * - 统计/埋点 SDK 启动
 * - 预加载数据
 * - 非首屏必需的 UI 初始化
 *
 * 使用示例：
 * ```kotlin
 * // 注册空闲任务
 * IdleTaskExecutor.addIdleTask(priority = 10) {
 *     // 高优先级：广告 SDK 初始化
 *     AdSDK.init(context)
 * }
 *
 * IdleTaskExecutor.addIdleTask(priority = 1) {
 *     // 低优先级：推送 SDK 注册
 *     PushSDK.register(context, "sender_id")
 * }
 *
 * // 启动监听（在 Application.onCreate 中调用）
 * IdleTaskExecutor.start()
 * ```
 *
 * 注意事项：
 * - [start] 必须在主线程调用
 * - 任务会在主线程执行，禁止执行耗时操作
 * - 如果任务需要耗时，应在任务内部切换到后台线程
 * - IdleHandler 在首次主线程空闲时触发
 */
object IdleTaskExecutor {

    private const val TAG = "IdleTaskExecutor"

    /**
     * 空闲任务数据类
     *
     * @property priority 优先级（值越大优先级越高）
     * @property name 任务名称（用于日志）
     * @property block 任务执行逻辑
     */
    private data class IdleTask(
        val priority: Int,
        val name: String,
        val block: () -> Unit
    ) : Comparable<IdleTask> {
        override fun compareTo(other: IdleTask): Int {
            // 高优先级先执行（降序）
            return other.priority.compareTo(this.priority)
        }
    }

    /**
     * 待执行的空闲任务队列（按优先级排序）
     */
    private val taskQueue = PriorityBlockingQueue<IdleTask>()

    /**
     * 是否已启动监听
     */
    private val isStarted = AtomicBoolean(false)

    /**
     * IdleHandler 是否已注册
     */
    private val isHandlerRegistered = AtomicBoolean(false)

    /**
     * 添加空闲任务
     *
     * 任务会在主线程空闲时按优先级顺序执行。
     * 可在任意线程调用此方法。
     *
     * @param priority 优先级（值越大优先级越高，默认 0）
     * @param name 任务名称（用于日志，默认 "IdleTask-N"）
     * @param block 任务执行逻辑
     */
    fun addIdleTask(
        priority: Int = 0,
        name: String = "IdleTask-${System.currentTimeMillis()}",
        block: () -> Unit
    ) {
        taskQueue.offer(IdleTask(priority, name, block))
        Logger.d(TAG, "注册空闲任务: $name (优先级: $priority)")
    }

    /**
     * 启动空闲任务监听
     *
     * 注册 IdleHandler 到主线程消息队列。
     * 必须在主线程调用。
     *
     * 调用后，每当主线程空闲时，会依次执行已注册的任务。
     * 每次 Idle 回调只执行一个任务，避免长时间占用主线程。
     */
    fun start() {
        if (!isStarted.compareAndSet(false, true)) {
            Logger.w(TAG, "IdleTaskExecutor 已经启动，忽略重复调用")
            return
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Logger.e(TAG, "start() 必须在主线程调用")
            isStarted.set(false)
            return
        }

        registerIdleHandler()
    }

    /**
     * 注册 IdleHandler 到主线程
     */
    private fun registerIdleHandler() {
        if (!isHandlerRegistered.compareAndSet(false, true)) {
            return
        }

        Looper.myQueue().addIdleHandler {
            // 取出最高优先级的任务执行
            val task = taskQueue.poll()

            if (task != null) {
                Logger.d(TAG, "主线程空闲，执行任务: ${task.name} (优先级: ${task.priority})")
                try {
                    task.block()
                    Logger.d(TAG, "空闲任务完成: ${task.name}")
                } catch (e: Exception) {
                    Logger.e(TAG, "空闲任务执行失败: ${task.name}", e)
                }
                // 返回 true：继续监听，等待下次空闲执行下一个任务
                true
            } else {
                // 没有待执行任务，继续监听（等待新任务注册后下次空闲执行）
                true
            }
        }

        Logger.d(TAG, "IdleHandler 已注册到主线程")
    }

    /**
     * 获取待执行的任务数量
     *
     * @return 队列中的任务数
     */
    fun getPendingTaskCount(): Int = taskQueue.size

    /**
     * 清空所有待执行的任务
     *
     * 不会取消已注册但正在执行的任务。
     */
    fun clearAllTasks() {
        val count = taskQueue.size
        taskQueue.clear()
        Logger.d(TAG, "已清空 $count 个空闲任务")
    }

    /**
     * 移除指定名称的任务
     *
     * @param name 任务名称
     * @return 是否成功移除
     */
    fun removeTask(name: String): Boolean {
        return taskQueue.removeIf { it.name == name }
    }
}
