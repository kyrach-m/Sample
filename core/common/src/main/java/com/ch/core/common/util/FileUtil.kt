package com.ch.core.common.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件操作工具类
 *
 * 提供文件/目录的创建、删除、拷贝、大小计算与格式化等常用操作。
 * 所有方法内部捕获异常，异常时返回安全默认值，绝不向外抛异常。
 *
 * 用法示例：
 * ```kotlin
 * // 获取应用私有缓存目录
 * val cacheDir = FileUtil.getAppCacheDir(context)
 *
 * // 获取缓存大小（格式化字符串）
 * val size = FileUtil.getCacheSizeFormatted(context) // "12.5 MB"
 *
 * // 清除缓存
 * FileUtil.clearCacheDir(context)
 *
 * // 拷贝文件
 * FileUtil.copyFile(srcFile, dstFile)
 *
 * // 格式化文件大小
 * FileUtil.formatFileSize(1024 * 1024 * 5) // "5.00 MB"
 * ```
 */
object FileUtil {

    // ==================== 目录获取 ====================

    /**
     * 获取应用私有缓存目录
     *
     * 对应 Android 的 `context.cacheDir`，系统空间不足时可能被自动清理。
     *
     * @param context Context
     * @return 缓存目录 File
     */
    fun getAppCacheDir(context: Context): File {
        return context.cacheDir
    }

    /**
     * 获取应用外部缓存目录
     *
     * 对应 `context.externalCacheDir`，位于 SD 卡，用户可手动清除。
     *
     * @param context Context
     * @return 外部缓存目录，不可用时返回 null
     */
    fun getExternalCacheDir(context: Context): File? {
        return context.externalCacheDir
    }

    /**
     * 获取应用私有文件目录
     *
     * 对应 `context.filesDir`，卸载应用时自动清除。
     *
     * @param context Context
     * @return 文件目录 File
     */
    fun getAppFilesDir(context: Context): File {
        return context.filesDir
    }

    // ==================== 大小计算 ====================

    /**
     * 计算目录总大小（字节）
     *
     * 递归遍历目录内所有文件，累加大小。
     *
     * @param dir 目录
     * @return 目录总大小（字节），异常返回 0
     */
    fun getDirSize(dir: File): Long {
        return try {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取应用缓存大小（格式化字符串）
     *
     * 同时计算内部缓存和外部缓存的总大小。
     *
     * @param context Context
     * @return 格式化后的大小字符串，如 "12.5 MB"
     */
    fun getCacheSizeFormatted(context: Context): String {
        val internalSize = getDirSize(context.cacheDir)
        val externalSize = context.externalCacheDir?.let { getDirSize(it) } ?: 0L
        return formatFileSize(internalSize + externalSize)
    }

    /**
     * 格式化文件大小为可读字符串
     *
     * 根据大小自动选择合适的单位（B / KB / MB / GB）。
     *
     * @param sizeBytes 文件大小（字节）
     * @return 格式化字符串，如 "1.50 KB"、"3.20 MB"
     */
    fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "%.2f KB".format(sizeBytes / 1024.0)
            sizeBytes < 1024 * 1024 * 1024 -> "%.2f MB".format(sizeBytes / (1024.0 * 1024))
            else -> "%.2f GB".format(sizeBytes / (1024.0 * 1024 * 1024))
        }
    }

    // ==================== 创建 / 删除 ====================

    /**
     * 确保目录存在
     *
     * 如果目录不存在则递归创建。
     *
     * @param dir 目标目录
     * @return true = 目录已存在或创建成功
     */
    fun ensureDir(dir: File): Boolean {
        return try {
            if (dir.exists()) return dir.isDirectory
            dir.mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 确保文件存在
     *
     * 如果文件不存在，先确保父目录存在，再创建空文件。
     *
     * @param file 目标文件
     * @return true = 文件已存在或创建成功
     */
    fun ensureFile(file: File): Boolean {
        return try {
            if (file.exists()) return true
            ensureDir(file.parentFile ?: return false)
            file.createNewFile()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除文件或目录
     *
     * 如果是目录，递归删除所有子文件和子目录。
     *
     * @param file 要删除的文件或目录
     * @return true = 删除成功
     */
    fun delete(file: File): Boolean {
        return try {
            if (!file.exists()) return true
            if (file.isDirectory) {
                file.walkBottomUp().forEach { it.delete() }
            }
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 清除应用缓存目录
     *
     * 删除内部缓存和外部缓存中的所有文件。
     *
     * @param context Context
     * @return true = 全部清除成功
     */
    fun clearCacheDir(context: Context): Boolean {
        val internal = delete(context.cacheDir)
        val external = context.externalCacheDir?.let { delete(it) } ?: true
        return internal && external
    }

    // ==================== 拷贝 ====================

    /**
     * 拷贝文件
     *
     * 使用 FileInputStream/FileOutputStream 实现，适用于中小文件。
     * 目标文件不存在时会自动创建。
     *
     * @param src 源文件
     * @param dst 目标文件
     * @return true = 拷贝成功
     */
    fun copyFile(src: File, dst: File): Boolean {
        return try {
            ensureFile(dst)
            FileInputStream(src).use { input ->
                FileOutputStream(dst).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 拷贝目录
     *
     * 递归拷贝源目录中的所有文件和子目录到目标目录。
     *
     * @param srcDir 源目录
     * @param dstDir 目标目录
     * @return true = 拷贝成功
     */
    fun copyDir(srcDir: File, dstDir: File): Boolean {
        return try {
            if (!srcDir.exists() || !srcDir.isDirectory) return false
            ensureDir(dstDir)
            srcDir.listFiles()?.forEach { file ->
                val targetFile = File(dstDir, file.name)
                if (file.isDirectory) {
                    copyDir(file, targetFile)
                } else {
                    copyFile(file, targetFile)
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    // ==================== SD 卡状态 ====================

    /**
     * 检查外部存储（SD 卡）是否可用
     *
     * @return true = 外部存储已挂载且可读写
     */
    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}
