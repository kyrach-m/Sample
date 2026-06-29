package com.ch.core.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.ch.core.ui.R

/**
 * 流式布局（自动换行）
 *
 * 功能特性：
 * - 子 View 从左到右排列，超出宽度自动换行
 * - 适用于标签（Tag）展示场景
 * - 支持设置行间距和列间距
 * - 支持通过 XML 属性配置
 *
 * 自定义属性（attrs.xml）：
 * - fl_horizontalSpacing：水平间距（默认 8dp）
 * - fl_verticalSpacing：垂直间距（默认 8dp）
 *
 * 用法示例：
 * ```xml
 * <com.ch.core.ui.widget.FlowLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:fl_horizontalSpacing="8dp"
 *     app:fl_verticalSpacing="8dp">
 *
 *     <TextView
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="标签 1" />
 *
 *     <TextView
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="标签 2" />
 *
 * </com.ch.core.ui.widget.FlowLayout>
 * ```
 */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    /** 水平间距（px） */
    private var horizontalSpacingPx: Float

    /** 垂直间距（px） */
    private var verticalSpacingPx: Float

    /** 每行子 View 列表 */
    private val lines = mutableListOf<List<View>>()
    /** 每行高度 */
    private val lineHeights = mutableListOf<Int>()
    /** 每行宽度 */
    private val lineWidths = mutableListOf<Int>()

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout)
        horizontalSpacingPx = ta.getDimension(R.styleable.FlowLayout_fl_horizontalSpacing, dp2px(8f))
        verticalSpacingPx = ta.getDimension(R.styleable.FlowLayout_fl_verticalSpacing, dp2px(8f))
        ta.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        lines.clear()
        lineHeights.clear()
        lineWidths.clear()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val maxHeight = if (heightMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
        else MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        var currentLineWidth = 0
        var currentLineHeight = 0
        var currentLine = mutableListOf<View>()
        var totalHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            // 判断是否需要换行
            val newWidth = if (currentLine.isEmpty()) childWidth
            else currentLineWidth + horizontalSpacingPx.toInt() + childWidth

            if (newWidth > maxWidth && currentLine.isNotEmpty()) {
                // 换行
                lines.add(currentLine)
                lineHeights.add(currentLineHeight)
                lineWidths.add(currentLineWidth)
                totalHeight += currentLineHeight + verticalSpacingPx.toInt()
                currentLine = mutableListOf()
                currentLineWidth = 0
                currentLineHeight = 0
            }

            currentLine.add(child)
            currentLineWidth = if (currentLine.size == 1) childWidth
            else currentLineWidth + horizontalSpacingPx.toInt() + childWidth
            currentLineHeight = currentLineHeight.coerceAtLeast(childHeight)
        }

        // 添加最后一行
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
            lineHeights.add(currentLineHeight)
            lineWidths.add(currentLineWidth)
            totalHeight += currentLineHeight
        }

        val measuredHeight = if (heightMode == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            (totalHeight + paddingTop + paddingBottom).coerceAtMost(maxHeight + paddingTop + paddingBottom)
        }

        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            measuredHeight
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var y = paddingTop
        for (i in lines.indices) {
            val line = lines[i]
            val lineHeight = lineHeights[i]
            var x = paddingLeft

            for (child in line) {
                child.layout(
                    x, y,
                    x + child.measuredWidth, y + child.measuredHeight
                )
                x += child.measuredWidth + horizontalSpacingPx.toInt()
            }
            y += lineHeight + verticalSpacingPx.toInt()
        }
    }

    private fun dp2px(dp: Float): Float = dp * context.resources.displayMetrics.density
}
