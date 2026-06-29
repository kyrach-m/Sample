package com.ch.core.ui.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.ch.core.ui.R

/**
 * RecyclerView 统一分割线装饰器
 *
 * 功能特性：
 * - 为列表提供统一的分割线
 * - 支持设置分割线颜色、高度
 * - 支持设置左右边距（inset）
 * - 支持仅显示在 item 之间（不显示首尾）
 * - 支持通过 XML 属性配置
 *
 * 自定义属性：通过代码构造传入参数。
 *
 * 用法示例：
 * ```kotlin
 * // 基础用法
 * recyclerView.addItemDecoration(
 *     DividerDecoration(context)
 * )
 *
 * // 自定义样式
 * recyclerView.addItemDecoration(
 *     DividerDecoration(context)
 *         .setColor(Color.parseColor("#E0E0E0"))
 *         .setHeight(dp2px(1f))
 *         .setInsetStart(dp2px(16f))
 *         .setInsetEnd(dp2px(16f))
 * )
 * ```
 */
class DividerDecoration(
    private val context: android.content.Context
) : RecyclerView.ItemDecoration() {

    /** 分割线颜色 */
    private var dividerColor: Int = Color.parseColor("#E0E0E0")

    /** 分割线高度（px） */
    private var dividerHeightPx: Float = context.resources.displayMetrics.density

    /** 左边距（px） */
    private var insetStartPx: Float = 0f

    /** 右边距（px） */
    private var insetEndPx: Float = 0f

    /** 是否隐藏最后一个 item 的分割线 */
    private var hideLast: Boolean = true

    /** 绘制画笔 */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        style = Paint.Style.FILL
    }

    /** 临时矩形 */
    private val rect = Rect()

    /**
     * 设置分割线颜色
     *
     * @param color 颜色值
     * @return this（链式调用）
     */
    fun setColor(color: Int): DividerDecoration {
        dividerColor = color
        paint.color = color
        return this
    }

    /**
     * 设置分割线高度
     *
     * @param height 高度（px）
     * @return this（链式调用）
     */
    fun setHeight(height: Float): DividerDecoration {
        dividerHeightPx = height
        return this
    }

    /**
     * 设置左边距
     *
     * @param inset 左边距（px）
     * @return this（链式调用）
     */
    fun setInsetStart(inset: Float): DividerDecoration {
        insetStartPx = inset
        return this
    }

    /**
     * 设置右边距
     *
     * @param inset 右边距（px）
     * @return this（链式调用）
     */
    fun setInsetEnd(inset: Float): DividerDecoration {
        insetEndPx = inset
        return this
    }

    /**
     * 设置是否隐藏最后一个 item 的分割线
     *
     * @param hide 是否隐藏
     * @return this（链式调用）
     */
    fun setHideLast(hide: Boolean): DividerDecoration {
        hideLast = hide
        return this
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        if (position < itemCount - 1 || !hideLast) {
            outRect.set(0, 0, 0, dividerHeightPx.toInt())
        }
    }

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val childCount = parent.childCount
        val adapterCount = state.itemCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)

            // 跳过最后一个 item（如果设置了 hideLast）
            if (hideLast && position >= adapterCount - 1) continue

            val left = (child.left + insetStartPx).toInt()
            val right = (child.right - insetEndPx).toInt()
            val top = child.bottom
            val bottom = top + dividerHeightPx.toInt()

            rect.set(left, top, right, bottom)
            canvas.drawRect(rect, paint)
        }
    }
}
