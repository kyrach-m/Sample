package com.ch.core.ui.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.ch.core.ui.R

/**
 * 加载对话框
 *
 * 功能特性：
 * - 中央显示圆形 ProgressBar + 文案
 * - 不可取消（防止用户误关闭）
 * - 支持动态更新加载文案
 * - 与 LoadingMask 的区别：仅覆盖对话框区域而非全屏
 *
 * 用法示例：
 * ```kotlin
 * val dialog = LoadingDialog(context)
 * dialog.showLoading("上传中…")
 * // ... 业务完成后
 * dialog.dismissLoading()
 * ```
 *
 * 注意：
 * - showLoading() 内部自动调用 show()
 * - dismissLoading() 内部自动调用 dismiss()
 * - 默认不可通过返回键取消
 */
class LoadingDialog(
    context: Context
) : BaseDialog(context) {

    /** 加载文案 TextView */
    private val tvLoadingMsg: TextView

    init {
        // 替换为加载布局
        val contentView = View.inflate(context, R.layout.layout_dialog_loading, null)
        setCustomView(contentView)
        tvLoadingMsg = contentView.findViewById(R.id.tv_loading_msg)

        // 默认不可取消
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    /**
     * 显示加载对话框
     *
     * @param msg 加载提示文案，默认"加载中…"
     */
    fun showLoading(msg: String = context.getString(R.string.core_ui_loading)) {
        tvLoadingMsg.text = msg
        if (!isShowing) {
            show()
        }
    }

    /**
     * 隐藏加载对话框
     */
    fun dismissLoading() {
        if (isShowing) {
            dismiss()
        }
    }

    /**
     * 更新加载文案（不关闭对话框）
     *
     * @param msg 新的加载提示
     */
    fun updateMessage(msg: String) {
        tvLoadingMsg.text = msg
    }
}
