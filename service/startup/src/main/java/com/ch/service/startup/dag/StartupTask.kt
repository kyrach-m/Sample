package com.ch.service.startup.dag

import android.content.Context

/**
 * 启动任务抽象类
 *
 * 所有启动任务必须继承此类，并实现 [name] 和 [execute] 方法。
 * 通过 [dependencies] 声明依赖关系，由 [TaskGraph] 构建 DAG 并确定执行顺序。
 *
 * 任务分类：
 * - 关键路径任务：[runOnMainThread] = true，必须在主线程执行（如 UI 初始化）
 * - 后台任务：[runOnMainThread] = false，可在线程池并行执行
 * - 高优先级任务：[priority] 值越大优先级越高
 *
 * 使用示例：
 * ```kotlin
 * class NetworkTask : StartupTask() {
 *     override val name = "NetworkTask"
 *     override fun execute(context: Context) {
 *         NetworkClient.init(context)
 *     }
 * }
 * ```
 *
 * @property name 任务唯一标识名称，用于日志和监控
 */
abstract class StartupTask {

    /**
     * 任务名称（唯一标识）
     *
     * 用于日志输出、耗时监控和调试。
     * 必须全局唯一，重复名称会在注册时抛出异常。
     */
    abstract val name: String

    /**
     * 执行任务逻辑
     *
     * 由 [StartupScheduler] 在合适的线程中调用。
     * - 如果 [runOnMainThread] 返回 true，在主线程执行
     * - 否则在线程池中并行执行
     *
     * @param context Application Context
     */
    abstract fun execute(context: Context)

    /**
     * 声明当前任务依赖的其他任务
     *
     * 返回的任务列表中的任务一定在当前任务之前执行完毕。
     * 依赖关系用于构建 DAG，决定执行顺序。
     *
     * 注意：
     * - 不能形成循环依赖，否则 [TaskGraph] 构建时抛出 [CyclicDependencyException]
     * - 依赖的任务必须已通过 [StartupScheduler.addTask] 注册
     *
     * @return 依赖的任务类列表，默认为空
     */
    open fun dependencies(): List<Class<out StartupTask>> = emptyList()

    /**
     * 是否必须在主线程执行
     *
     * 返回 true 时，[execute] 在主线程调用，适用于：
     * - UI 相关初始化
     * - 需要在 Application.onCreate() 中完成的任务
     *
     * 返回 false 时，[execute] 在线程池中调用，适用于：
     * - 网络配置
     * - 数据库初始化
     * - 文件 IO
     *
     * @return true 表示主线程执行，默认 false
     */
    open fun runOnMainThread(): Boolean = false

    /**
     * 任务优先级
     *
     * 值越大优先级越高。当多个任务同时可执行时，高优先级任务先执行。
     * 同优先级任务的执行顺序不确定（取决于线程调度）。
     *
     * @return 优先级值，默认 0
     */
    open fun priority(): Int = 0

    /**
     * 任务执行超时时间（毫秒）
     *
     * 超过此时间后，[StartupScheduler] 会记录警告日志并继续执行后续任务，
     * 不会无限等待。设置为 0 表示不限制超时。
     *
     * @return 超时时间（毫秒），默认 5000ms
     */
    open fun timeoutMs(): Long = 5000L
}
