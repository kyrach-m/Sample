package com.ch.service.logger

import android.util.Log
import com.ch.core.common.logger.LoggerImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 应用日志实现
 *
 * 实现 [LoggerImpl] 接口，提供完整的日志功能：
 * - Debug/Info/Warning 仅在 Debug 模式输出
 * - Error 始终输出
 * - 自动附加调用位置信息（类名#方法名:行号）
 * - 内存环形缓冲区，保留最近 500 条日志（崩溃时导出）
 * - 文件输出增强：通过 [LogFileManager] 写入文件
 *
 * 通过 `Logger.init(AppLoggerImpl(context, debug))` 注入使用。
 *
 * @param context Application Context
 * @param isDebug 是否为 Debug 模式
 */
class AppLoggerImpl(
    private val context: android.content.Context,
    private val isDebug: Boolean
) : LoggerImpl {

    companion object {
        /** 调用栈中 Logger 类的帧偏移量 */
        private const val CALL_STACK_INDEX = 4

        /** 环形缓冲区最大容量 */
        private const val RING_BUFFER_SIZE = 500

        /** 最大 TAG 长度（Android Logcat 限制） */
        private const val MAX_TAG_LENGTH = 23
    }

    /**
     * 日期格式化器（Logcat TAG 用）
     */
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 日期格式化器（文件日志用，完整日期时间）
     */
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 日志条目数据类
     *
     * @property timestamp 时间戳
     * @property level 日志级别（D/I/W/E）
     * @property tag 日志标签
     * @property message 日志消息
     */
    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        /**
         * 格式化为可读字符串
         */
        fun format(): String {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(Date(timestamp))
            return "$time [$level/$tag] $message"
        }
    }

    /**
     * 环形缓冲区（线程安全）
     */
    private val ringBuffer = ConcurrentLinkedQueue<LogEntry>()

    /**
     * 日志文件管理器
     */
    private val logFileManager: LogFileManager? = try {
        LogFileManager(context.applicationContext)
    } catch (e: Exception) {
        Log.e("AppLoggerImpl", "LogFileManager 初始化失败", e)
        null
    }

    init {
        if (isDebug) {
            Log.d("AppLoggerImpl", "Logger 初始化完成 (Debug 模式)")
        }
    }

    override fun d(tag: String, msg: String) {
        if (!isDebug) return
        val location = getCallLocation()
        val fullTag = buildFullTag(tag)
        val fullMsg = "[$location] $msg"
        Log.d(fullTag, fullMsg)
        addToBuffer("D", tag, fullMsg)
    }

    override fun i(tag: String, msg: String) {
        if (!isDebug) return
        val location = getCallLocation()
        val fullTag = buildFullTag(tag)
        val fullMsg = "[$location] $msg"
        Log.i(fullTag, fullMsg)
        addToBuffer("I", tag, fullMsg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        if (!isDebug) return
        val location = getCallLocation()
        val fullTag = buildFullTag(tag)
        val fullMsg = "[$location] $msg"
        if (tr != null) {
            Log.w(fullTag, fullMsg, tr)
        } else {
            Log.w(fullTag, fullMsg)
        }
        val bufferMsg = if (tr != null) {
            "$fullMsg\n${Log.getStackTraceString(tr)}"
        } else {
            fullMsg
        }
        addToBuffer("W", tag, bufferMsg)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        val location = getCallLocation()
        val fullTag = buildFullTag(tag)
        val fullMsg = "[$location] $msg"
        if (tr != null) {
            Log.e(fullTag, fullMsg, tr)
        } else {
            Log.e(fullTag, fullMsg)
        }
        val bufferMsg = if (tr != null) {
            "$fullMsg\n${Log.getStackTraceString(tr)}"
        } else {
            fullMsg
        }
        addToBuffer("E", tag, bufferMsg)
    }

    override fun dumpRecentLogs(maxCount: Int): String {
        val entries = ringBuffer.toList()
        return entries.takeLast(maxCount).joinToString("\n") { it.format() }
    }

    /**
     * 获取环形缓冲区中的日志条目列表
     *
     * @return 日志条目列表（快照）
     */
    fun getRecentEntries(): List<LogEntry> {
        return ringBuffer.toList()
    }

    /**
     * 添加日志到环形缓冲区并写入文件
     *
     * @param level 日志级别
     * @param tag 原始标签
     * @param message 完整消息
     */
    private fun addToBuffer(level: String, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        ringBuffer.offer(entry)

        // 环形缓冲区溢出保护
        while (ringBuffer.size > RING_BUFFER_SIZE) {
            ringBuffer.poll()
        }

        // 写入日志文件
        try {
            logFileManager?.write(entry)
        } catch (e: Exception) {
            // 文件写入失败不影响主流程
        }
    }

    /**
     * 构建完整 TAG
     */
    private fun buildFullTag(tag: String): String {
        val time = timeFormat.format(Date())
        val fullTag = "$time | $tag"
        return if (fullTag.length > MAX_TAG_LENGTH) {
            fullTag.substring(0, MAX_TAG_LENGTH)
        } else {
            fullTag
        }
    }

    /**
     * 获取调用位置信息
     */
    private fun getCallLocation(): String {
        return try {
            val stackTrace = Thread.currentThread().stackTrace
            if (stackTrace.size > CALL_STACK_INDEX) {
                val element = stackTrace[CALL_STACK_INDEX]
                val className = element.fileName?.substringBefore(".kt")
                    ?.substringBefore(".java") ?: "Unknown"
                "${className}#${element.methodName}:${element.lineNumber}"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
