package com.ch.service.startup.initializer

import android.content.Context
import android.os.Looper
import com.ch.core.common.logger.Logger
import androidx.startup.Initializer

/**
 * 延迟初始化器
 *
 * 通过 AndroidX Startup 框架自动执行，但内部使用 IdleHandler
 * 将实际初始化逻辑延迟到主线程空闲时执行，避免阻塞启动流程。
 *
 * 适用场景：
 * - 广告 SDK 初始化
 * - 推送 SDK 初始化
 * - 统计/埋点 SDK 初始化
 * - 其他非关键路径的第三方 SDK
 *
 * 执行时机：
 * - create() 在主线程执行，但通过 IdleHandler 延迟到主线程空闲时
 * - 无依赖，与 NetworkInitializer 并行注册
 * - 实际业务逻辑在首个 Idle 回调中执行
 *
 * 依赖关系：
 * - 无依赖
 *
 * 注意：
 * - IdleHandler 在首次主线程空闲时触发，通常在 Application.onCreate() 完成后
 * - 禁止在 IdleHandler 中执行耗时操作（应异步执行）
 * - 广告/推送 SDK 的具体初始化代码需业务层自行填充
 *
 * 用法：
 * 无需手动调用，由 AndroidX Startup 自动执行。
 * 如需手动触发，调用：
 * ```kotlin
 * AppInitializer.getInstance(context)
 *     .initializeComponent(DelayedInitializer::class.java)
 * ```
 */
class DelayedInitializer : Initializer<Unit> {

    companion object {
        private const val TAG = "DelayedInitializer"
    }

    /**
     * 执行初始化
     *
     * 通过 Looper.myQueue().addIdleHandler 注册空闲回调，
     * 在主线程空闲时执行延迟初始化逻辑。
     *
     * @param context Application Context
     */
    override fun create(context: Context) {
        Looper.myQueue().addIdleHandler {
            Logger.d(TAG, "主线程空闲，开始延迟初始化...")

            // ========================================
            // TODO: 广告 SDK 初始化
            // ========================================
            // 示例：
            // AdSDK.init(context.applicationContext)
            // AdSDK.setAppId("your_app_id")
            // AdSDK.start()

            // ========================================
            // TODO: 推送 SDK 初始化
            // ========================================
            // 示例：
            // PushSDK.init(context.applicationContext)
            // PushSDK.register("your_sender_id")

            // ========================================
            // TODO: 统计/埋点 SDK 初始化
            // ========================================
            // 示例：
            // AnalyticsSDK.init(context.applicationContext, "your_tracking_id")

            Logger.d(TAG, "延迟初始化完成")

            // 返回 false：只执行一次，执行后自动移除
            false
        }
    }

    /**
     * 声明依赖
     *
     * 返回空列表，表示无依赖。
     *
     * @return 空的依赖列表
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
