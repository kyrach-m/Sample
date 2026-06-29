package com.ch.core.ui.snackbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.ch.core.ui.R
import com.google.android.material.snackbar.Snackbar

/**
 * 顶部 Snackbar（轻量级顶部通知）
 *
 * 功能特性：
 * - 将 Snackbar 显示位置从底部改为顶部
 * - 支持自定义背景色
 * - 支持设置图标
 * - 支持 Action 按钮
 * - 自动适配深色模式
 *
 * 用法示例：
 * ```kotlin
 * // 基础用法
 * TopSnackbar.make(view, "操作成功", TopSnackbar.LENGTH_SHORT)
 *     .setBackgroundColor(getColor(R.color.success))
 *     .show()
 *
 * // 带图标和 Action
 * TopSnackbar.make(view, "已删除 1 项", TopSnackbar.LENGTH_LONG)
 *     .setIcon(R.drawable.ic_undo)
 *     .setAction("撤销") { undoDelete() }
 *     .setBackgroundColor(getColor(R.color.info))
 *     .show()
 * ```
 */
class TopSnackbar private constructor(
    private val snackbar: Snackbar
) {

    companion object {
        /** 短时长 */
        const val LENGTH_SHORT = Snackbar.LENGTH_SHORT
        /** 长时长 */
        const val LENGTH_LONG = Snackbar.LENGTH_LONG
        /** 永久显示 */
        const val LENGTH_INDEFINITE = Snackbar.LENGTH_INDEFINITE

        /**
         * 创建顶部 Snackbar
         *
         * @param parent 父 View（用于查找 CoordinatorLayout 或 DecorView）
         * @param message 通知内容
         * @param duration 显示时长
         * @return TopSnackbar 实例
         */
        fun make(parent: View, message: CharSequence, duration: Int): TopSnackbar {
            val customView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_top_snackbar, parent as ViewGroup, false)

            val tvMessage = customView.findViewById<TextView>(R.id.tv_snackbar_message)
            tvMessage.text = message

            val snackbar = Snackbar.make(parent, "", duration)
            val snackbarLayout = snackbar.view as ViewGroup
            snackbarLayout.removeAllViews()
            snackbarLayout.addView(customView)

            // 设置背景透明（使用自定义布局的背景色）
            snackbar.view.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            return TopSnackbar(snackbar)
        }
    }

    /**
     * 设置背景颜色
     *
     * @param color 背景色
     * @return this（链式调用）
     */
    fun setBackgroundColor(@ColorInt color: Int): TopSnackbar {
        val customView = (snackbar.view as ViewGroup).getChildAt(0)
        customView.setBackgroundColor(color)
        return this
    }

    /**
     * 设置图标
     *
     * @param iconRes 图标资源 ID
     * @return this（链式调用）
     */
    fun setIcon(@DrawableRes iconRes: Int): TopSnackbar {
        val customView = (snackbar.view as ViewGroup).getChildAt(0)
        val ivIcon = customView.findViewById<ImageView>(R.id.iv_snackbar_icon)
        ivIcon.setImageResource(iconRes)
        ivIcon.visibility = View.VISIBLE
        return this
    }

    /**
     * 设置 Action 按钮
     *
     * @param text Action 文字
     * @param onClick 点击回调
     * @return this（链式调用）
     */
    fun setAction(text: String, onClick: () -> Unit): TopSnackbar {
        val customView = (snackbar.view as ViewGroup).getChildAt(0)
        val tvAction = customView.findViewById<TextView>(R.id.tv_snackbar_action)
        tvAction.text = text
        tvAction.visibility = View.VISIBLE
        tvAction.setOnClickListener {
            onClick()
            snackbar.dismiss()
        }
        return this
    }

    /**
     * 显示顶部通知
     */
    fun show() {
        snackbar.show()
    }

    /**
     * 关闭通知
     */
    fun dismiss() {
        snackbar.dismiss()
    }
}
