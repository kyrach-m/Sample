package com.ch.service.logger

import android.content.Context
import android.util.Log
import com.ch.core.common.logger.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志加密接口
 *
 * 业务层可实现此接口，自定义加密算法（如 AES、ChaCha20）。
 * 通过 [setEncryption] 注入到 [LogFileManager]。
 */
fun interface LogEncryptor {
    /**
     * 加密日志内容
     *
     * @param plainText 明文日志
     * @return 加密后的字节数组
     */
    fun encrypt(plainText: String): ByteArray
}

/**
 * 日志文件管理器
 *
 * 负责将日志写入本地文件，支持文件滚动和过期清理。
 *
 * 核心特性：
 * - 日志写入目录：{filesDir}/logger/
 * - 当前日志文件：app.log
 * - 文件大小超过 2MB 时自动滚动，重命名为 app_yyyyMMdd_HHmmss.log
 * - 保留最近 7 天的日志文件，超过 7 天自动删除
 * - 提供 [zipLogs] 方法，将日志压缩为 zip 用于导出或上报
 * - 异步写入（单线程队列），不阻塞主线程
 *
 * 使用方式：
 * ```kotlin
 * // 由 Logger.init() 自动初始化，无需手动创建
 * // 手动使用：
 * val manager = LogFileManager(context)
 * manager.write(logEntry)
 * val zipFile = manager.zipLogs() // 压缩所有日志
 * ```
 */
class LogFileManager(context: Context) {

    companion object {
        private const val TAG = "LogFileManager"
        private const val LOG_DIR = "logger"
        private const val CURRENT_LOG = "app.log"
        private const val MAX_FILE_SIZE = 2L * 1024 * 1024 // 2MB
        private const val RETENTION_DAYS = 7L
    }

    /**
     * 日志目录
     */
    private val logDir: File

    /**
     * 当前日志文件
     */
    private var currentFile: File

    /**
     * 异步写入队列
     */
    private val writeQueue = LinkedBlockingQueue<AppLoggerImpl.LogEntry>(1000)

    /**
     * 写入线程
     */
    @Volatile
    private var writeThread: Thread? = null

    /**
     * 日志加密器（可选）
     *
     * 设置后，日志写入文件时会先加密。
     */
    @Volatile
    private var encryptor: LogEncryptor? = null

    /**
     * 日期格式化器（文件名用，ThreadLocal 保证线程安全）
     */
    private val fileNameFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }

    /**
     * 日期格式化器（日志内容用，ThreadLocal 保证线程安全）
     */
    private val logContentFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    }

    init {
        logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        currentFile = File(logDir, CURRENT_LOG)
        startWriteThread()
        cleanExpiredLogs()
    }

    /**
     * 设置日志加密器
     *
     * 设置后，所有写入文件的日志都会先加密。
     * 导出/压缩的日志也会是加密状态。
     *
     * @param encryptor 加密器实现，传 null 取消加密
     */
    fun setEncryption(encryptor: LogEncryptor?) {
        this.encryptor = encryptor
        Logger.d(TAG, if (encryptor != null) "日志加密已启用" else "日志加密已关闭")
    }

    /**
     * 写入一条日志
     *
     * 日志会加入异步写入队列，由后台线程实际写入文件。
     * 如果队列已满（1000 条），会丢弃最早的日志。
     *
     * @param entry 日志条目
     */
    fun write(entry: AppLoggerImpl.LogEntry) {
        if (!writeQueue.offer(entry)) {
            writeQueue.poll() // 丢弃最早的
            writeQueue.offer(entry)
        }
    }

    /**
     * 获取当前日志文件大小
     *
     * @return 文件大小（字节）
     */
    fun getCurrentFileSize(): Long {
        return if (currentFile.exists()) currentFile.length() else 0L
    }

    /**
     * 获取所有日志文件列表
     *
     * @return 日志文件列表（按修改时间降序）
     */
    fun getLogFiles(): List<File> {
        return logDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 压缩日志文件
     *
     * 将所有日志文件（包括当前日志）压缩为 zip 文件。
     * 用于导出或上报时节省流量。
     *
     * @param outputFile 输出的 zip 文件路径，默认为 {logDir}/logs.zip
     * @return 压缩后的 zip 文件，失败返回 null
     */
    fun zipLogs(outputFile: File = File(logDir, "logs.zip")): File? {
        return try {
            // 先刷新当前文件
            val files = getLogFiles()
            if (files.isEmpty()) {
                Logger.d(TAG, "没有日志文件可压缩")
                return null
            }

            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                val buffer = ByteArray(8192)
                for (file in files) {
                    if (!file.exists() || file.length() == 0L) continue
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { fis ->
                        var len: Int
                        while (fis.read(buffer).also { len = it } > 0) {
                            zos.write(buffer, 0, len)
                        }
                    }
                    zos.closeEntry()
                }
            }

            Logger.d(TAG, "日志压缩完成: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "日志压缩失败", e)  // 内部错误仍用 Log 防止循环
            null
        }
    }

    /**
     * 清理过期日志
     *
     * 删除超过 [RETENTION_DAYS] 天的日志文件。
     */
    fun cleanExpiredLogs() {
        try {
            val expireTime = System.currentTimeMillis() - RETENTION_DAYS * 24 * 60 * 60 * 1000
            logDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".log") && file.name != CURRENT_LOG) {
                    if (file.lastModified() < expireTime) {
                        file.delete()
                        Logger.d(TAG, "已删除过期日志: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "清理过期日志失败", e)
        }
    }

    /**
     * 启动异步写入线程
     */
    private fun startWriteThread() {
        writeThread = Thread({
            while (true) {
                try {
                    val entry = writeQueue.take() // 阻塞等待
                    writeToFile(entry)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Logger.e(TAG, "日志写入线程异常", e)
                }
            }
        }, "Logger-FileWriter").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * 将日志条目写入文件
     *
     * 写入前检查文件大小，超过 2MB 时滚动。
     *
     * @param entry 日志条目
     */
    @Synchronized
    private fun writeToFile(entry: AppLoggerImpl.LogEntry) {
        // 检查文件大小，需要滚动
        if (currentFile.exists() && currentFile.length() >= MAX_FILE_SIZE) {
            rollFile()
        }

        try {
            val time = logContentFormat.get().format(Date(entry.timestamp))
            val line = "$time [${entry.level}/${entry.tag}] ${entry.message}\n"
            val enc = encryptor
            if (enc != null) {
                // 加密写入
                val encrypted = enc.encrypt(line)
                FileOutputStream(currentFile, true).use { fos ->
                    fos.write(encrypted)
                }
            } else {
                // 明文写入
                OutputStreamWriter(FileOutputStream(currentFile, true), Charsets.UTF_8).use { writer ->
                    writer.write(line)
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "日志文件写入失败", e)
        }
    }

    /**
     * 滚动日志文件
     *
     * 将当前 app.log 重命名为 app_yyyyMMdd_HHmmss.log，
     * 然后创建新的 app.log。
     */
    @Synchronized
    private fun rollFile() {
        try {
            val timestamp = fileNameFormat.get().format(Date())
            val rolledFile = File(logDir, "app_$timestamp.log")
            if (currentFile.renameTo(rolledFile)) {
                currentFile = File(logDir, CURRENT_LOG)
                Logger.d(TAG, "日志文件滚动: ${rolledFile.name}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "日志文件滚动失败", e)
        }
    }
}
