package com.ch.core.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.ch.core.ui.R
import com.google.android.material.button.MaterialButton

/**
 * 统一样式基础对话框
 *
 * 功能特性：
 * - 圆角背景、统一内边距
 * - 支持 setTitle() / setMessage() 设置标题和内容
 * - 支持 setPositiveButton() / setNegativeButton() 链式调用
 * - 支持 setCustomView() 设置自定义内容
 * - 自动适配深色模式（通过主题颜色引用）
 *
 * 用法示例：
 * ```kotlin
 * BaseDialog(context)
 *     .setTitle("提示")
 *     .setMessage("确定要删除吗？")
 *     .setPositiveButton("确定") { dialog -> dialog.dismiss() }
 *     .setNegativeButton("取消") { dialog -> dialog.dismiss() }
 *     .show()
 * ```
 */
open class BaseDialog(
    context: Context,
    themeResId: Int = R.style.CoreDialogStyle
) : Dialog(context, themeResId) {

    /** 标题 TextView */
    protected val tvTitle: TextView
    /** 内容 TextView */
    protected val tvMessage: TextView
    /** 按钮容器 */
    protected val buttonContainer: LinearLayout
    /** 根视图 */
    protected val rootView: View

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        rootView = LayoutInflater.from(context).inflate(R.layout.layout_dialog_base, null)
        setContentView(rootView)

        // 设置窗口背景透明
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        tvTitle = rootView.findViewById(R.id.tv_dialog_title)
        tvMessage = rootView.findViewById(R.id.tv_dialog_message)
        buttonContainer = rootView.findViewById(R.id.layout_dialog_buttons)
    }

    /**
     * 设置对话框标题
     *
     * @param title 标题文字
     * @return this（链式调用）
     */
    fun setTitle(title: String): BaseDialog {
        tvTitle.text = title
        tvTitle.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置对话框内容
     *
     * @param message 内容文字
     * @return this（链式调用）
     */
    fun setMessage(message: String): BaseDialog {
        tvMessage.text = message
        tvMessage.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置确认按钮（右侧）
     *
     * @param text 按钮文字
     * @param onClick 点击回调
     * @return this（链式调用）
     */
    fun setPositiveButton(text: String, onClick: (Dialog) -> Unit): BaseDialog {
        buttonContainer.visibility = View.VISIBLE
        val btn = MaterialButton(context).apply {
            this.text = text
            setOnClickListener { onClick(this@BaseDialog) }
        }
        buttonContainer.addView(btn)
        return this
    }

    /**
     * 设置取消按钮（左侧）
     *
     * @param text 按钮文字
     * @param onClick 点击回调
     * @return this（链式调用）
     */
    fun setNegativeButton(text: String, onClick: (Dialog) -> Unit): BaseDialog {
        buttonContainer.visibility = View.VISIBLE
        val btn = MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            this.text = text
            setTextColor(context.getColor(R.color.on_background))
            setOnClickListener { onClick(this@BaseDialog) }
        }
        buttonContainer.addView(btn, 0)
        return this
    }

    /**
     * 设置自定义内容视图
     *
     * 替换默认内容区域。
     *
     * @param view 自定义 View
     * @return this（链式调用）
     */
    fun setCustomView(view: View): BaseDialog {
        val parent = rootView as ViewGroup
        // 在 title 和 buttons 之间插入
        val index = parent.indexOfChild(buttonContainer)
        parent.addView(view, index)
        return this
    }
}
