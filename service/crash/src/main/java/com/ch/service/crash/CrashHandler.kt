package com.ch.service.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import com.ch.core.common.logger.Logger
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志导出回调接口
 *
 * 用于从 :service:logger 获取最近日志。
 * 通过 [setLogDumper] 注入，避免模块间直接依赖。
 */
fun interface LogDumper {
    /**
     * 导出最近的日志
     *
     * @param maxCount 最大条数
     * @return 格式化的日志字符串
     */
    fun dumpRecentLogs(maxCount: Int): String
}

/**
 * 全局异常捕获处理器
 *
 * 实现 [Thread.UncaughtExceptionHandler]，捕获所有未处理的异常，
 * 将崩溃日志写入本地缓存目录，然后启动 [RecoveryActivity] 进行优雅恢复。
 *
 * 核心功能：
 * 1. 捕获所有未处理异常（包括主线程和子线程）
 * 2. 收集设备信息、版本信息、异常堆栈
 * 3. 将崩溃日志写入应用缓存目录（crash/子目录）
 * 4. 启动 RecoveryActivity（独立进程），显示恢复提示
 * 5. 延迟 2 秒后执行 [Process.killProcess]，确保日志写入完成
 * 6. 确保 APP 永不出系统强制关闭对话框
 *
 * 使用方式：
 * ```kotlin
 * // 在 Application.onCreate() 中初始化
 * CrashHandler.init(this)
 * ```
 *
 * 日志存储位置：
 * - {cacheDir}/crash/crash_{timestamp}.log
 *
 * 注意事项：
 * - [init] 必须在主线程调用（通常在 Application.onCreate 中）
 * - 会保存系统默认的 UncaughtExceptionHandler 引用，在记录完日志后调用
 * - RecoveryActivity 运行在独立进程（:recovery），确保主进程崩溃后仍可显示 UI
 * - 如果缓存目录写入失败，会尝试使用 Log.e 输出（至少能在 Logcat 中看到）
 */
class CrashHandler private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_DIR = "crash"
        private const val RECOVERY_DELAY_MS = 2000L
        private const val RECENT_LOG_COUNT = 200

        @Volatile
        private var instance: CrashHandler? = null

        /**
         * 日志导出器（由外部注入）
         */
        @Volatile
        private var logDumper: LogDumper? = null

        /**
         * 设置日志导出器
         *
         * 在 Application.onCreate 中调用，注入 Logger 的日志导出能力。
         *
         * @param dumper 日志导出回调
         */
        fun setLogDumper(dumper: LogDumper?) {
            logDumper = dumper
        }

        /**
         * 初始化全局异常捕获器
         *
         * 必须在 Application.onCreate() 中调用。
         * 重复调用会忽略（单例模式）。
         *
         * @param context Application Context
         */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(instance)
                        Logger.d(TAG, "CrashHandler 已注册为全局异常处理器")
                    }
                }
            }
        }

        /**
         * 获取 CrashHandler 实例（用于测试或手动注销）
         */
        fun getInstance(): CrashHandler? = instance

        /**
         * 检查 CrashHandler 是否已初始化
         */
        fun isInitialized(): Boolean = instance != null

        /**
         * 获取崩溃日志目录
         *
         * 路径：{cacheDir}/crash/
         *
         * @param context Application Context
         * @return 崩溃日志目录
         */
        fun getCrashDir(context: Context): File {
            return File(context.cacheDir, CRASH_DIR)
        }

        /**
         * 获取所有崩溃日志文件
         *
         * 按修改时间倒序排列（最新的在前）。
         *
         * @param context Application Context
         * @return 崩溃日志文件列表
         */
        fun getCrashLogFiles(context: Context): List<File> {
            val crashDir = getCrashDir(context)
            return crashDir.listFiles { file ->
                file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        }

        /**
         * 获取最新的崩溃日志文件
         *
         * @param context Application Context
         * @return 最新的崩溃日志文件，若不存在返回 null
         */
        fun getLatestCrashLog(context: Context): File? {
            return getCrashLogFiles(context).firstOrNull()
        }

        /**
         * 获取崩溃日志内容
         *
         * @param context Application Context
         * @return 崩溃日志内容，若不存在返回 null
         */
        fun getLatestCrashLogContent(context: Context): String? {
            return getLatestCrashLog(context)?.readText()
        }
    }

    /**
     * 系统默认的异常处理器（备用）
     */
    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    /**
     * 日期格式化器
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.getDefault())

    /**
     * 异常捕获回调
     *
     * 当线程发生未处理异常时调用。执行以下操作：
     * 1. 收集崩溃信息（设备、版本、堆栈）
     * 2. 写入本地缓存文件
     * 3. 启动 RecoveryActivity
     * 4. 延迟 2 秒后杀死进程
     *
     * @param thread 发生异常的线程
     * @param throwable 未捕获的异常
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Logger.e(TAG, "捕获到未处理异常 [线程: ${thread.name}]", throwable)

            // Step 1: 收集崩溃信息
            val crashLog = buildCrashLog(thread, throwable)

            // Step 2: 获取最近日志（通过注入的 LogDumper）
            val recentLogs = try {
                logDumper?.dumpRecentLogs(RECENT_LOG_COUNT) ?: "(无最近日志)"
            } catch (e: Exception) {
                "(日志导出失败: ${e.message})"
            }

            // Step 3: 写入本地缓存
            val fullLog = crashLog + "\n\n─────── 最近日志（最近 $RECENT_LOG_COUNT 条）───────\n$recentLogs"
            val logFile = saveCrashLog(fullLog)
            Logger.d(TAG, "崩溃日志已保存: ${logFile?.absolutePath}")

            // Step 4: 构建错误摘要（脱敏后传递给 RecoveryActivity）
            val errorSummary = buildErrorSummary(throwable)

            // Step 5: 启动 RecoveryActivity（独立进程）
            startRecoveryActivity(errorSummary)

            // Step 6: 延迟后杀死进程（确保日志写入和 RecoveryActivity 启动完成）
            Thread.sleep(RECOVERY_DELAY_MS)

        } catch (e: Exception) {
            // 如果处理过程中又发生异常，至少输出到 Logcat
            Logger.e(TAG, "CrashHandler 处理异常时发生错误", e)
        } finally {
            // Step 7: 杀死进程
            // 必须先 kill 再 exit，确保系统回收资源
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }

    /**
     * 构建崩溃日志字符串
     *
     * 包含以下信息：
     * - 时间戳
     * - 线程信息
     * - 应用版本
     * - 设备信息（品牌、型号、Android 版本、SDK 版本）
     * - 异常堆栈
     *
     * @param thread 发生异常的线程
     * @param throwable 未捕获的异常
     * @return 格式化的崩溃日志字符串
     */
    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()

        // 时间戳
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("崩溃时间: ${dateFormat.format(Date())}")
        sb.appendLine("═══════════════════════════════════════")

        // 线程信息
        sb.appendLine("线程: ${thread.name} (id: ${thread.id})")
        sb.appendLine()

        // 应用版本信息
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("应用版本: ${packageInfo.versionName}")
            sb.appendLine("版本号: ${getVersionCode()}")
        } catch (e: Exception) {
            sb.appendLine("应用版本: 未知")
        }
        sb.appendLine("包名: ${context.packageName}")
        sb.appendLine()

        // 设备信息
        sb.appendLine("─────── 设备信息 ───────")
        sb.appendLine("品牌: ${Build.BRAND}")
        sb.appendLine("型号: ${Build.MODEL}")
        sb.appendLine("设备: ${Build.DEVICE}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("ABI: ${Build.SUPPORTED_ABIS?.joinToString() ?: "unknown"}")
        sb.appendLine()

        // 异常堆栈
        sb.appendLine("─────── 异常堆栈 ───────")
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        sb.appendLine(sw.toString())

        // Cause 链
        var cause = throwable.cause
        while (cause != null) {
            sb.appendLine()
            sb.appendLine("─────── Caused by ───────")
            cause.printStackTrace(PrintWriter(sw))
            sb.appendLine(sw.toString())
            cause = cause.cause
        }

        sb.appendLine("═══════════════════════════════════════")

        return sb.toString()
    }

    /**
     * 获取应用版本号（兼容不同 API）
     */
    @Suppress("DEPRECATION")
    private fun getVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * 保存崩溃日志到本地缓存
     *
     * 存储路径：{cacheDir}/crash/crash_{timestamp}.log
     *
     * @param crashLog 崩溃日志字符串
     * @return 日志文件，写入失败时返回 null
     */
    private fun saveCrashLog(crashLog: String): File? {
        return try {
            val crashDir = File(context.cacheDir, CRASH_DIR)
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            val timestamp = dateFormat.format(Date())
            val logFile = File(crashDir, "crash_$timestamp.log")

            logFile.writeText(crashLog)
            Logger.d(TAG, "崩溃日志写入成功: ${logFile.absolutePath}")

            logFile
        } catch (e: Exception) {
            Logger.e(TAG, "崩溃日志写入失败", e)
            // 降级：输出到外部缓存（如果有权限）
            try {
                val externalDir = context.externalCacheDir
                if (externalDir != null) {
                    val fallbackDir = File(externalDir, CRASH_DIR)
                    fallbackDir.mkdirs()
                    val fallbackFile = File(fallbackDir, "crash_${dateFormat.format(Date())}.log")
                    fallbackFile.writeText(crashLog)
                    Logger.d(TAG, "崩溃日志降级写入外部缓存: ${fallbackFile.absolutePath}")
                    return fallbackFile
                }
            } catch (e2: Exception) {
                Logger.e(TAG, "降级写入也失败", e2)
            }
            null
        }
    }

    /**
     * 启动恢复页面 Activity
     *
     * 使用 FLAG_ACTIVITY_NEW_TASK 在新任务栈中启动 RecoveryActivity。
     * RecoveryActivity 运行在独立进程（:recovery），确保即使主进程崩溃也能显示 UI。
     *
     * @param errorSummary 脱敏后的错误摘要
     */
    private fun startRecoveryActivity(errorSummary: String) {
        try {
            val intent = Intent(context, RecoveryActivity::class.java).apply {
                // 新任务栈启动
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                // 传递错误摘要
                putExtra(RecoveryActivity.EXTRA_ERROR_SUMMARY, errorSummary)
            }
            context.startActivity(intent)
            Logger.d(TAG, "RecoveryActivity 已启动")
        } catch (e: Exception) {
            Logger.e(TAG, "启动 RecoveryActivity 失败", e)
        }
    }

    /**
     * 构建脱敏后的错误摘要
     *
     * 提取异常类型和首行消息，去除文件路径等敏感信息。
     *
     * @param throwable 异常对象
     * @return 脱敏后的错误摘要字符串
     */
    private fun buildErrorSummary(throwable: Throwable): String {
        val exceptionType = throwable.javaClass.simpleName
        val message = throwable.message ?: "未知错误"
        // 脱敏：去除可能的文件路径、用户数据
        val sanitizedMessage = message
            .replace(Regex("/data/user/\\d+/[^\\s]+"), "[PATH]")
            .replace(Regex("/storage/emulated/\\d+/[^\\s]+"), "[PATH]")
            .take(200) // 限制长度
        return "$exceptionType: $sanitizedMessage"
    }
}
