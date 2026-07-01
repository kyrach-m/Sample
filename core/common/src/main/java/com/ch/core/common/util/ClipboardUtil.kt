package com.ch.core.common.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * 剪贴板工具类
 *
 * 提供文本复制到剪贴板、从剪贴板读取文本、监听剪贴板变化等功能。
 * 所有方法内部捕获异常，异常时返回安全默认值。
 *
 * 用法示例：
 * ```kotlin
 * // 复制文本到剪贴板
 * ClipboardUtil.copyText(context, "邀请码：ABC123")
 *
 * // 复制文本并指定 label
 * ClipboardUtil.copyText(context, "ABC123", label = "邀请码")
 *
 * // 获取剪贴板文本
 * val text = ClipboardUtil.getText(context)
 *
 * // 判断剪贴板是否有文本
 * if (ClipboardUtil.hasText(context)) { ... }
 *
 * // 监听剪贴板变化
 * ClipboardUtil.addPrimaryClipChangedListener(context) {
 *     val newText = ClipboardUtil.getText(context)
 *     // 处理剪贴板内容变化
 * }
 * ```
 */
object ClipboardUtil {

    /**
     * 获取系统剪贴板管理器
     *
     * @param context Context
     * @return ClipboardManager 实例
     */
    private fun getManager(context: Context): ClipboardManager {
        return context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // ==================== 复制 ====================

    /**
     * 复制文本到剪贴板
     *
     * @param context Context
     * @param text 要复制的文本
     * @param label 剪贴板标签（用户在剪贴板管理器中可见的描述），默认 "label"
     * @return true = 复制成功
     */
    fun copyText(context: Context, text: String, label: String = "label"): Boolean {
        return try {
            val clipData = ClipData.newPlainText(label, text)
            getManager(context).setPrimaryClip(clipData)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 复制 URI 到剪贴板
     *
     * @param context Context
     * @param uri 要复制的 URI 字符串
     * @param label 剪贴板标签
     * @return true = 复制成功
     */
    fun copyUri(context: Context, uri: String, label: String = "uri"): Boolean {
        return try {
            val clipData = ClipData.newUri(context.contentResolver, label, android.net.Uri.parse(uri))
            getManager(context).setPrimaryClip(clipData)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== 读取 ====================

    /**
     * 获取剪贴板文本内容
     *
     * 如果剪贴板为空或内容不是文本类型，返回空字符串。
     *
     * @param context Context
     * @return 剪贴板文本内容，无内容时返回 ""
     */
    fun getText(context: Context): String {
        return try {
            val manager = getManager(context)
            if (!manager.hasPrimaryClip()) return ""
            val clipDescription = manager.primaryClipDescription ?: return ""
            // 仅当内容包含文本 MIME 类型时才读取
            if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                manager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 判断剪贴板是否有文本内容
     *
     * @param context Context
     * @return true = 剪贴板包含文本
     */
    fun hasText(context: Context): Boolean {
        return try {
            val manager = getManager(context)
            manager.hasPrimaryClip() &&
                    manager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true &&
                    (manager.primaryClip?.getItemAt(0)?.text?.isNotEmpty() == true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取剪贴板标签（label）
     *
     * @param context Context
     * @return 剪贴板标签，无内容时返回 null
     */
    fun getLabel(context: Context): String? {
        return try {
            getManager(context).primaryClipDescription?.label?.toString()
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 监听 ====================

    /**
     * 添加剪贴板内容变化监听器
     *
     * 当剪贴板内容发生变化时触发回调。
     * 注意：需要在适当时机调用 [removePrimaryClipChangedListener] 移除监听，避免内存泄漏。
     *
     * @param context Context
     * @param listener 剪贴板变化回调
     */
    fun addPrimaryClipChangedListener(
        context: Context,
        listener: ClipboardManager.OnPrimaryClipChangedListener
    ) {
        try {
            getManager(context).addPrimaryClipChangedListener(listener)
        } catch (e: Exception) {
            // 静默处理
        }
    }

    /**
     * 移除剪贴板内容变化监听器
     *
     * @param context Context
     * @param listener 要移除的监听器
     */
    fun removePrimaryClipChangedListener(
        context: Context,
        listener: ClipboardManager.OnPrimaryClipChangedListener
    ) {
        try {
            getManager(context).removePrimaryClipChangedListener(listener)
        } catch (e: Exception) {
            // 静默处理
        }
    }

    /**
     * 清空剪贴板
     *
     * Android 10+ 使用 setPrimaryClip(ClipData.newPlainText("", "")) 清空。
     * Android 9 及以下使用 clearPrimaryClip()。
     *
     * @param context Context
     */
    fun clear(context: Context) {
        try {
            val manager = getManager(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.clearPrimaryClip()
            } else {
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (e: Exception) {
            // 静默处理
        }
    }
}
