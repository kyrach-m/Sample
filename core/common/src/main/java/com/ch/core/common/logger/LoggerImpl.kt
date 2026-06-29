package com.ch.core.common.logger

/**
 * 日志实现接口
 *
 * 由 :service:logger 模块提供具体实现，通过 [Logger.init] 注入。
 * 各业务模块仅依赖此接口，不关心具体实现细节。
 *
 * 实现方可自由扩展：文件写入、日志滚动、远程上报等能力。
 */
interface LoggerImpl {

    /**
     * 输出 Debug 级别日志
     *
     * @param tag 日志标签
     * @param msg 日志消息
     */
    fun d(tag: String, msg: String)

    /**
     * 输出 Info 级别日志
     *
     * @param tag 日志标签
     * @param msg 日志消息
     */
    fun i(tag: String, msg: String)

    /**
     * 输出 Warning 级别日志
     *
     * @param tag 日志标签
     * @param msg 日志消息
     * @param tr 异常对象（可选）
     */
    fun w(tag: String, msg: String, tr: Throwable? = null)

    /**
     * 输出 Error 级别日志
     *
     * @param tag 日志标签
     * @param msg 日志消息
     * @param tr 异常对象（可选）
     */
    fun e(tag: String, msg: String, tr: Throwable? = null)

    /**
     * 导出最近的日志条目
     *
     * 用于崩溃时附加最近日志，帮助定位问题。
     *
     * @param maxCount 最大导出条数
     * @return 格式化的日志字符串
     */
    fun dumpRecentLogs(maxCount: Int = 200): String = ""
}
