package com.ch.core.ui.widget.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

/**
 * 全局对话框组件
 *
 * 基于 AlertDialog 封装的统一对话框组件，
 * 提供 Builder 模式构建对话框。
 *
 * 用法示例：
 * ```kotlin
 * GlobalDialog.Builder(context)
 *     .setTitle("提示")
 *     .setMessage("确定要删除吗？")
 *     .setPositiveButton("确定") { }
 *     .setNegativeButton("取消") { }
 *     .show()
 * ```
 */
class GlobalDialog(private val dialog: AlertDialog) {

    /**
     * 显示对话框
     */
    fun show() {
        dialog.show()
    }

    /**
     * 隐藏对话框
     */
    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    /**
     * 对话框构建器
     */
    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var positiveText: String? = null
        private var positiveListener: (() -> Unit)? = null
        private var negativeText: String? = null
        private var negativeListener: (() -> Unit)? = null
        private var cancelable: Boolean = true

        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        fun setPositiveButton(text: String, listener: (() -> Unit)? = null): Builder {
            this.positiveText = text
            this.positiveListener = listener
            return this
        }

        fun setNegativeButton(text: String, listener: (() -> Unit)? = null): Builder {
            this.negativeText = text
            this.negativeListener = listener
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun show(): GlobalDialog {
            val builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(cancelable)

            if (positiveText != null) {
                builder.setPositiveButton(positiveText) { _, _ ->
                    positiveListener?.invoke()
                }
            }

            if (negativeText != null) {
                builder.setNegativeButton(negativeText) { _, _ ->
                    negativeListener?.invoke()
                }
            }

            val dialog = builder.create()
            val globalDialog = GlobalDialog(dialog)
            dialog.show()
            return globalDialog
        }
    }
}
