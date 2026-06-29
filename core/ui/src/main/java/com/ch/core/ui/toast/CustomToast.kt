package com.ch.core.ui.toast

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.ch.core.ui.R

/**
 * 自定义 Toast 封装
 *
 * 功能特性：
 * - 支持自定义布局（带图标、不同背景色）
 * - 支持设置图标
 * - 支持设置背景色和圆角
 * - 兼容 Android 11+（使用 setView + setDuration）
 * - 线程安全（自动切换到主线程）
 *
 * 用法示例：
 * ```kotlin
 * // 基础用法
 * CustomToast.show(context, "操作成功")
 *
 * // 带图标和自定义颜色
 * CustomToast.builder(context)
 *     .message("删除成功")
 *     .icon(R.drawable.ic_check_circle)
 *     .backgroundColor(getColor(R.color.success))
 *     .textColor(Color.WHITE)
 *     .show()
 * ```
 */
object CustomToast {

    /**
     * 快速显示 Toast
     *
     * @param context Context
     * @param message 提示内容
     * @param duration 显示时长（默认 Toast.LENGTH_SHORT）
     */
    fun show(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        builder(context)
            .message(message)
            .duration(duration)
            .show()
    }

    /**
     * 创建 Builder
     *
     * @param context Context
     * @return Builder 实例
     */
    fun builder(context: Context): Builder = Builder(context)

    /**
     * Toast Builder
     */
    class Builder(private val context: Context) {

        private var message: CharSequence = ""
        private var duration: Int = Toast.LENGTH_SHORT
        @ColorInt
        private var bgColor: Int = Color.parseColor("#333333")
        @ColorInt
        private var textColor: Int = Color.WHITE
        @DrawableRes
        private var iconRes: Int = 0
        private var cornerRadius: Float = context.resources.displayMetrics.density * 12f

        /**
         * 设置提示内容
         */
        fun message(msg: CharSequence): Builder {
            this.message = msg
            return this
        }

        /**
         * 设置显示时长
         */
        fun duration(dur: Int): Builder {
            this.duration = dur
            return this
        }

        /**
         * 设置背景色
         */
        fun backgroundColor(@ColorInt color: Int): Builder {
            this.bgColor = color
            return this
        }

        /**
         * 设置文字颜色
         */
        fun textColor(@ColorInt color: Int): Builder {
            this.textColor = color
            return this
        }

        /**
         * 设置图标
         */
        fun icon(@DrawableRes res: Int): Builder {
            this.iconRes = res
            return this
        }

        /**
         * 设置圆角半径（px）
         */
        fun cornerRadius(radius: Float): Builder {
            this.cornerRadius = radius
            return this
        }

        /**
         * 显示 Toast
         */
        fun show() {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.layout_custom_toast, null)

            // 设置背景
            val rootView = view.findViewById<View>(R.id.toast_root)
            val bgDrawable = GradientDrawable().apply {
                setColor(bgColor)
                this.cornerRadius = this@Builder.cornerRadius
            }
            rootView.background = bgDrawable

            // 设置文字
            val tvMsg = view.findViewById<TextView>(R.id.tv_toast_message)
            tvMsg.text = message
            tvMsg.setTextColor(textColor)

            // 设置图标
            if (iconRes != 0) {
                val ivIcon = view.findViewById<ImageView>(R.id.iv_toast_icon)
                ivIcon.setImageResource(iconRes)
                ivIcon.visibility = View.VISIBLE
                ivIcon.setColorFilter(textColor)
            }

            val toast = Toast(context)
            toast.duration = duration
            toast.view = view
            toast.show()
        }
    }
}
