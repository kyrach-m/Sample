package com.ch.core.ui.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.ch.core.ui.R
import com.google.android.material.button.MaterialButton

/**
 * 确认对话框
 *
 * 功能特性：
 * - 标准"确认/取消"双按钮布局
 * - 支持自定义标题、内容、按钮文案
 * - 支持确认/取消回调
 * - 自动适配深色模式
 *
 * 用法示例：
 * ```kotlin
 * ConfirmDialog(context)
 *     .setConfirmTitle("删除确认")
 *     .setConfirmMessage("确定要删除该记录吗？此操作不可恢复。")
 *     .setPositiveText("删除")
 *     .setNegativeText("取消")
 *     .onConfirm {
 *         // 执行删除
 *     }
 *     .onCancel {
 *         // 取消操作
 *     }
 *     .show()
 * ```
 */
class ConfirmDialog(
    context: Context
) : BaseDialog(context) {

    /** 标题 */
    private val tvConfirmTitle: TextView
    /** 内容 */
    private val tvConfirmMessage: TextView
    /** 确认按钮 */
    private val btnPositive: MaterialButton
    /** 取消按钮 */
    private val btnNegative: MaterialButton

    /** 确认回调 */
    private var confirmAction: (() -> Unit)? = null
    /** 取消回调 */
    private var cancelAction: (() -> Unit)? = null

    init {
        val contentView = View.inflate(context, R.layout.layout_dialog_confirm, null)
        setCustomView(contentView)

        tvConfirmTitle = contentView.findViewById(R.id.tv_confirm_title)
        tvConfirmMessage = contentView.findViewById(R.id.tv_confirm_message)
        btnPositive = contentView.findViewById(R.id.btn_positive)
        btnNegative = contentView.findViewById(R.id.btn_negative)

        // 隐藏 BaseDialog 自带的按钮容器
        // (ConfirmDialog 使用自己的布局)
    }

    /**
     * 设置标题
     *
     * @param title 标题文字
     * @return this（链式调用）
     */
    fun setConfirmTitle(title: String): ConfirmDialog {
        tvConfirmTitle.text = title
        return this
    }

    /**
     * 设置内容
     *
     * @param message 内容文字
     * @return this（链式调用）
     */
    fun setConfirmMessage(message: String): ConfirmDialog {
        tvConfirmMessage.text = message
        return this
    }

    /**
     * 设置确认按钮文案
     *
     * @param text 按钮文字（默认"确认"）
     * @return this（链式调用）
     */
    fun setPositiveText(text: String): ConfirmDialog {
        btnPositive.text = text
        return this
    }

    /**
     * 设置取消按钮文案
     *
     * @param text 按钮文字（默认"取消"）
     * @return this（链式调用）
     */
    fun setNegativeText(text: String): ConfirmDialog {
        btnNegative.text = text
        return this
    }

    /**
     * 设置确认回调
     *
     * @param action 确认时执行
     * @return this（链式调用）
     */
    fun onConfirm(action: () -> Unit): ConfirmDialog {
        confirmAction = action
        btnPositive.setOnClickListener {
            action()
            dismiss()
        }
        return this
    }

    /**
     * 设置取消回调
     *
     * @param action 取消时执行
     * @return this（链式调用）
     */
    fun onCancel(action: () -> Unit): ConfirmDialog {
        cancelAction = action
        btnNegative.setOnClickListener {
            action()
            dismiss()
        }
        return this
    }
}
