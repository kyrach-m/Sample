package com.ch.core.common.logger

/**
 * 日志门面类
 *
 * 全项目统一日志入口。所有模块通过 `Logger.d/i/w/e` 输出日志，
 * 不直接使用 `android.util.Log`。
 *
 * 架构设计：
 * - 本类定义在 `:core:common`，提供轻量门面接口
 * - 具体实现由 `:service:logger` 的 `AppLoggerImpl` 提供
 * - 通过 [init] 方法在 Application.onCreate 中注入实现
 *
 * Release 行为：
 * - 注入的实现方可自行决定是否输出 Debug/Info/Warning 日志
 * - Error 级别始终输出
 *
 * 使用示例：
 * ```kotlin
 * Logger.d("Network", "请求成功")
 * Logger.e("Database", "查询失败", exception)
 * ```
 */
object Logger {

    /**
     * 空操作实现（init 前的安全兜底）
     *
     * 所有方法均为 no-op，确保 init 前调用不会崩溃。
     */
    private val NO_OP = object : LoggerImpl {
        override fun d(tag: String, msg: String) {}
        override fun i(tag: String, msg: String) {}
        override fun w(tag: String, msg: String, tr: Throwable?) {}
        override fun e(tag: String, msg: String, tr: Throwable?) {}
        override fun dumpRecentLogs(maxCount: Int): String = ""
    }

    /**
     * 当前日志实现
     *
     * 默认为 NO_OP，init 后替换为实际实现。
     * 使用 @Volatile 保证多线程可见性。
     */
    @Volatile
    private var impl: LoggerImpl = NO_OP

    /**
     * 注入日志实现
     *
     * 必须在 Application.onCreate() 中调用，且应最先初始化。
     *
     * @param loggerImpl 日志实现实例（通常由 :service:logger 提供）
     */
    fun init(loggerImpl: LoggerImpl) {
        impl = loggerImpl
    }

    /**
     * 输出 Debug 级别日志
     *
     * Release 包中是否输出取决于注入的实现。
     *
     * @param tag 日志标签
     * @param msg 日志消息
     */
    fun d(tag: String, msg: String) {
        impl.d(tag, msg)
    }

    /**
     * 输出 Info 级别日志
     *
     * Release 包中是否输出取决于注入的实现。
     *
     * @param tag 日志标签
     * @param msg 日志消息
     */
    fun i(tag: String, msg: String) {
        impl.i(tag, msg)
    }

    /**
     * 输出 Warning 级别日志
     *
     * Release 包中是否输出取决于注入的实现。
     *
     * @param tag 日志标签
     * @param msg 日志消息
     * @param tr 异常对象（可选）
     */
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        impl.w(tag, msg, tr)
    }

    /**
     * 输出 Error 级别日志
     *
     * **始终输出**，不受 Debug/Release 模式影响。
     *
     * @param tag 日志标签
     * @param msg 日志消息
     * @param tr 异常对象（可选）
     */
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        impl.e(tag, msg, tr)
    }

    /**
     * 导出最近的日志条目
     *
     * 用于崩溃时附加最近日志，帮助定位问题。
     *
     * @param maxCount 最大导出条数，默认 200
     * @return 格式化的日志字符串
     */
    fun dumpRecentLogs(maxCount: Int = 200): String {
        return impl.dumpRecentLogs(maxCount)
    }
}
