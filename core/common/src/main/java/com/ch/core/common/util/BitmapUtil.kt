package com.ch.core.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 图片处理工具类
 *
 * 提供图片压缩、缩放、格式转换等常用操作。
 * 所有方法内部捕获异常，异常时返回 null 或 false，绝不向外抛异常。
 *
 * ## 压缩策略
 * - 质量压缩：降低 JPEG/WebP 质量，减小文件体积（不改变尺寸）
 * - 尺寸压缩：按比例缩小图片宽高，减少内存占用
 * - 采样压缩：使用 inSampleSize 在解码阶段降采样，最省内存
 *
 * 用法示例：
 * ```kotlin
 * // 质量压缩（压缩到 80% 质量）
 * val compressed = BitmapUtil.compressQuality(bitmap, 80)
 *
 * // 尺寸压缩（缩放到 50%）
 * val scaled = BitmapUtil.scale(bitmap, 0.5f)
 *
 * // 限制最大尺寸（超过 1920x1080 自动等比缩放）
 * val bounded = BitmapUtil.scaleToBounds(bitmap, 1920, 1080)
 *
 * // 保存为文件
 * BitmapUtil.saveToFile(bitmap, File(cacheDir, "photo.jpg"), Bitmap.CompressFormat.JPEG, 85)
 *
 * // 计算合适的 inSampleSize
 * val sampleSize = BitmapUtil.calculateInSampleSize(4000, 3000, 800, 600) // 返回 4
 * ```
 *
 * @see <a href="https://developer.android.com/topic/performance/graphics">Android 图形性能指南</a>
 */
object BitmapUtil {

    // ==================== 质量压缩 ====================

    /**
     * 质量压缩
     *
     * 不改变图片尺寸，仅降低编码质量以减小文件体积。
     * 适用于 JPEG/WebP 格式，PNG 为无损格式不受质量影响。
     *
     * @param bitmap 原始 Bitmap
     * @param quality 压缩质量（0-100），80 为推荐值
     * @param format 输出格式，默认 JPEG
     * @return 压缩后的字节数组，失败返回 null
     */
    fun compressQuality(
        bitmap: Bitmap,
        quality: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): ByteArray? {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(format, quality.coerceIn(0, 100), stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 质量压缩并写入文件
     *
     * @param bitmap 原始 Bitmap
     * @param file 目标文件
     * @param format 输出格式
     * @param quality 压缩质量（0-100）
     * @return true = 保存成功
     */
    fun saveToFile(
        bitmap: Bitmap,
        file: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 85
    ): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { stream ->
                bitmap.compress(format, quality.coerceIn(0, 100), stream)
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    // ==================== 尺寸压缩 ====================

    /**
     * 按比例缩放
     *
     * @param bitmap 原始 Bitmap
     * @param scale 缩放比例（0.5 = 缩小一半，2.0 = 放大两倍）
     * @return 缩放后的 Bitmap，失败返回 null
     */
    fun scale(bitmap: Bitmap, scale: Float): Bitmap? {
        return try {
            val width = (bitmap.width * scale).toInt()
            val height = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 缩放到指定尺寸
     *
     * @param bitmap 原始 Bitmap
     * @param targetWidth 目标宽度（px）
     * @param targetHeight 目标高度（px）
     * @return 缩放后的 Bitmap，失败返回 null
     */
    fun scaleToSize(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 限制最大边界（等比缩放）
     *
     * 如果图片宽高超过指定上限，则等比缩小到上限范围内。
     * 如果图片已在范围内，直接返回原图（不创建新对象）。
     *
     * @param bitmap 原始 Bitmap
     * @param maxWidth 最大宽度（px）
     * @param maxHeight 最大高度（px）
     * @return 处理后的 Bitmap
     */
    fun scaleToBounds(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 旋转图片
     *
     * @param bitmap 原始 Bitmap
     * @param degrees 旋转角度（0/90/180/270）
     * @return 旋转后的 Bitmap，失败返回 null
     */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap? {
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 采样压缩（解码阶段） ====================

    /**
     * 计算合适的 inSampleSize
     *
     * 根据原始尺寸和目标尺寸，计算 2 的幂次采样率。
     * 用于 BitmapFactory.Options.inSampleSize，在解码阶段降采样以节省内存。
     *
     * @param rawWidth 原始宽度
     * @param rawHeight 原始高度
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @return inSampleSize 值（2 的幂次，如 1/2/4/8）
     */
    fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > targetHeight || rawWidth > targetWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 采样压缩解码文件
     *
     * 先读取图片边界信息，计算合适的 inSampleSize，再解码为低分辨率 Bitmap。
     * 适用于加载大图时避免 OOM。
     *
     * @param filePath 图片文件路径
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @return 降采样后的 Bitmap，失败返回 null
     */
    fun decodeSampledFromFile(filePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            // 第一步：仅读取边界
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, options)

            // 第二步：计算 inSampleSize
            options.inSampleSize = calculateInSampleSize(
                options.outWidth, options.outHeight, targetWidth, targetHeight
            )
            options.inJustDecodeBounds = false

            // 第三步：解码
            BitmapFactory.decodeFile(filePath, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 采样压缩解码 Uri
     *
     * @param context Context
     * @param uri 图片 Uri
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @return 降采样后的 Bitmap，失败返回 null
     */
    fun decodeSampledFromUri(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)

                options.inSampleSize = calculateInSampleSize(
                    options.outWidth, options.outHeight, targetWidth, targetHeight
                )
                options.inJustDecodeBounds = false

                val resetStream = context.contentResolver.openInputStream(uri) ?: return null
                resetStream.use { BitmapFactory.decodeStream(it, null, options) }
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 格式转换 ====================

    /**
     * Bitmap 转 JPEG 字节数组
     *
     * @param bitmap Bitmap
     * @param quality JPEG 质量（0-100）
     * @return JPEG 字节数组，失败返回 null
     */
    fun toJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray? {
        return compressQuality(bitmap, quality, Bitmap.CompressFormat.JPEG)
    }

    /**
     * Bitmap 转 PNG 字节数组
     *
     * @param bitmap Bitmap
     * @return PNG 字节数组，失败返回 null
     */
    fun toPng(bitmap: Bitmap): ByteArray? {
        return compressQuality(bitmap, 100, Bitmap.CompressFormat.PNG)
    }

    /**
     * Bitmap 转 WebP 字节数组
     *
     * @param bitmap Bitmap
     * @param quality WebP 质量（0-100）
     * @return WebP 字节数组，失败返回 null
     */
    fun toWebp(bitmap: Bitmap, quality: Int = 85): ByteArray? {
        return compressQuality(bitmap, quality, Bitmap.CompressFormat.WEBP)
    }
}
