package com.ch.core.ui.widget

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.ch.core.ui.R

/**
 * 圆角 ImageView
 *
 * 功能特性：
 * - 支持通过 XML 属性设置圆角半径
 * - 支持圆形裁剪和圆角矩形裁剪
 * - 使用 ViewOutlineProvider 实现硬件加速裁剪
 *
 * 自定义属性（attrs.xml）：
 * - riv_radius：圆角半径（默认 0dp，即无圆角）
 * -riv_shape：形状类型（rectangle/circle，默认 rectangle）
 *
 * 用法示例：
 * ```xml
 * <!-- 圆角矩形 -->
 * <com.ch.core.ui.widget.RoundImageView
 *     android:layout_width="100dp"
 *     android:layout_height="100dp"
 *     android:src="@drawable/photo"
 *     app:riv_radius="8dp" />
 *
 * <!-- 圆形 -->
 * <com.ch.core.ui.widget.RoundImageView
 *     android:layout_width="100dp"
 *     android:layout_height="100dp"
 *     android:src="@drawable/avatar"
 *     app:riv_shape="circle" />
 * ```
 */
class RoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /** 圆角半径（px） */
    private var radiusPx: Float = 0f

    /** 形状类型 */
    private var shape: Shape = Shape.RECTANGLE

    /**
     * 形状枚举
     */
    enum class Shape {
        /** 圆角矩形 */
        RECTANGLE,
        /** 圆形 */
        CIRCLE
    }

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.RoundImageView)
            radiusPx = ta.getDimension(R.styleable.RoundImageView_riv_radius, 0f)
            val shapeInt = ta.getInt(R.styleable.RoundImageView_riv_shape, 0)
            shape = if (shapeInt == 1) Shape.CIRCLE else Shape.RECTANGLE
            ta.recycle()
        }
        applyClip()
    }

    /**
     * 设置圆角半径
     *
     * @param radius 圆角半径（px）
     */
    fun setRadius(radius: Float) {
        this.radiusPx = radius
        this.shape = Shape.RECTANGLE
        applyClip()
    }

    /**
     * 设置形状
     *
     * @param shape 形状类型
     */
    fun setShape(shape: Shape) {
        this.shape = shape
        applyClip()
    }

    /**
     * 应用裁剪
     */
    private fun applyClip() {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                when (shape) {
                    Shape.CIRCLE -> {
                        val size = view.width.coerceAtMost(view.height)
                        val left = (view.width - size) / 2
                        val top = (view.height - size) / 2
                        outline.setOval(left, top, left + size, top + size)
                    }
                    Shape.RECTANGLE -> {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
            }
        }
    }
}
