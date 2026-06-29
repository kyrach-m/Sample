package com.ch.core.ui.badge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ch.core.ui.R

/**
 * 角标视图（红点 + 数字）
 *
 * 功能特性：
 * - 在右上角显示角标（红点或数字）
 * - 数字为 0 时自动隐藏
 * - 数字 > 99 时显示 "99+"
 * - 支持自定义背景色、文字颜色、大小
 * - 支持通过代码或 XML 属性配置
 * - 支持 attachToView() 动态附加到任意 View
 *
 * 自定义属性（attrs.xml）：
 * - bv_badgeColor：角标背景色（默认红色）
 * - bv_textColor：角标文字颜色（默认白色）
 * - bv_textSize：角标文字大小（默认 10sp）
 * - bv_badgeGravity：角标位置（默认右上角）
 *
 * 用法示例：
 * ```xml
 * <!-- XML 中使用 -->
 * <com.ch.core.ui.badge.BadgeView
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     app:bv_badgeColor="#FF0000"
 *     app:bv_textColor="#FFFFFF" />
 * ```
 *
 * ```kotlin
 * // 代码中使用
 * badgeView.setBadgeCount(5)
 * badgeView.setBadgeCount(0)  // 自动隐藏
 * badgeView.setBadgeCount(150) // 显示 "99+"
 *
 * // 附加到任意 View
 * BadgeView.attachToView(targetView, 3)
 * ```
 */
class BadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 角标背景色 */
    private var badgeColor: Int = Color.RED

    /** 文字颜色 */
    private var textColor: Int = Color.WHITE

    /** 文字大小（px） */
    private var textSizePx: Float = sp2px(10f)

    /** 当前数字 */
    private var count: Int = 0

    /** 绘制画笔 */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = badgeColor
    }

    /** 文字画笔 */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = textSizePx
    }

    /** 临时矩形 */
    private val rectF = RectF()

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.BadgeView)
            badgeColor = ta.getColor(R.styleable.BadgeView_bv_badgeColor, badgeColor)
            textColor = ta.getColor(R.styleable.BadgeView_bv_textColor, textColor)
            textSizePx = ta.getDimension(R.styleable.BadgeView_bv_textSize, textSizePx)
            ta.recycle()
        }

        bgPaint.color = badgeColor
        textPaint.color = textColor
        textPaint.textSize = textSizePx

        // 默认隐藏
        visibility = GONE
    }

    /**
     * 设置角标数字
     *
     * - 0：隐藏角标
     * - 1~99：显示数字
     * - >99：显示 "99+"
     *
     * @param count 角标数字
     */
    fun setBadgeCount(count: Int) {
        this.count = count
        visibility = if (count > 0) VISIBLE else GONE
        invalidate()
    }

    /**
     * 获取当前角标数字
     */
    fun getBadgeCount(): Int = count

    /**
     * 设置角标背景色
     *
     * @param color 背景色
     */
    fun setBadgeColor(color: Int) {
        badgeColor = color
        bgPaint.color = color
        invalidate()
    }

    /**
     * 设置角标文字颜色
     *
     * @param color 文字颜色
     */
    fun setBadgeTextColor(color: Int) {
        textColor = color
        textPaint.color = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val displayText = getDisplayText()
        val padding = dp2px(4f).toInt()

        if (displayText.isEmpty()) {
            // 红点模式
            val size = dp2px(8f).toInt() + padding * 2
            setMeasuredDimension(size, size)
        } else {
            val textWidth = textPaint.measureText(displayText).toInt()
            val textHeight = textPaint.textSize.toInt()
            val w = textWidth + padding * 2
            val h = textHeight + padding
            setMeasuredDimension(w.coerceAtLeast(h), h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val displayText = getDisplayText()

        if (displayText.isEmpty()) {
            // 红点模式：绘制圆形
            val radius = w.coerceAtMost(h) / 2f
            canvas.drawCircle(w / 2f, h / 2f, radius, bgPaint)
        } else {
            // 数字模式：绘制圆角矩形 + 文字
            val radius = h / 2f
            rectF.set(0f, 0f, w, h)
            canvas.drawRoundRect(rectF, radius, radius, bgPaint)

            // 绘制文字（垂直居中）
            val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(displayText, w / 2f, textY, textPaint)
        }
    }

    /**
     * 获取显示文字
     */
    private fun getDisplayText(): String {
        return when {
            count <= 0 -> ""
            count > 99 -> "99+"
            else -> count.toString()
        }
    }

    private fun dp2px(dp: Float): Float = dp * context.resources.displayMetrics.density
    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics
        )
    }

    companion object {
        /**
         * 附加角标到目标 View
         *
         * 在目标 View 的右上角添加角标。目标 View 的父容器必须是 FrameLayout。
         *
         * @param targetView 目标 View
         * @param count 角标数字
         * @return BadgeView 实例
         */
        fun attachToView(targetView: View, count: Int): BadgeView {
            val parent = targetView.parent as? ViewGroup
                ?: throw IllegalStateException("Target view must have a ViewGroup parent")

            val badge = BadgeView(targetView.context)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
            }

            parent.addView(badge, lp)
            badge.setBadgeCount(count)
            return badge
        }
    }
}
