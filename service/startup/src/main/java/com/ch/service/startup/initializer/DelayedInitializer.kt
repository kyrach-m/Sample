package com.ch.service.startup.initializer

import android.content.Context
import com.ch.core.common.logger.Logger
import com.ch.service.startup.idle.IdleTaskExecutor
import androidx.startup.Initializer

/**
 * 延迟初始化器
 *
 * 通过 AndroidX Startup 框架自动执行，但内部将任务注册到 [IdleTaskExecutor]，
 * 在主线程空闲时执行，避免阻塞启动流程。
 *
 * 设计说明：
 * - 统一使用 [IdleTaskExecutor] 作为空闲任务执行机制，避免重复注册 IdleHandler
 * - 业务层可通过 [IdleTaskExecutor.addIdleTask] 注册延迟任务
 * - 本初始化器仅作为 AndroidX Startup 与 [IdleTaskExecutor] 的桥梁
 *
 * 适用场景：
 * - 广告 SDK 初始化
 * - 推送 SDK 初始化
 * - 统计/埋点 SDK 初始化
 * - 其他非关键路径的第三方 SDK
 *
 * 依赖关系：
 * - 无依赖
 *
 * 用法：
 * 无需手动调用，由 AndroidX Startup 自动执行。
 * 注册延迟任务：
 * ```kotlin
 * IdleTaskExecutor.addIdleTask(priority = 10, name = "AdSDK") {
 *     AdSDK.init(context)
 * }
 * ```
 */
class DelayedInitializer : Initializer<Unit> {

    companion object {
        private const val TAG = "DelayedInitializer"
    }

    /**
     * 执行初始化
     *
     * 启动 [IdleTaskExecutor]，使其开始监听主线程空闲状态。
     * 已注册的空闲任务会在主线程空闲时按优先级执行。
     *
     * @param context Application Context
     */
    override fun create(context: Context) {
        Logger.d(TAG, "DelayedInitializer 启动，委托给 IdleTaskExecutor")
        IdleTaskExecutor.start()
    }

    /**
     * 声明依赖
     *
     * @return 空的依赖列表
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
