package com.ch.core.ui.skeleton

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.ch.core.ui.R

/**
 * 骨架屏加载控件
 *
 * 功能特性：
 * - 绘制多个灰色矩形占位块，模拟内容布局
 * - 闪烁/渐变动画效果，从左到右循环扫光
 * - 支持通过 attrs.xml 自定义骨架块颜色、高亮色、圆角、动画时长
 * - 提供 [startSkeleton] / [stopSkeleton] 控制动画
 * - 支持代码动态添加骨架块区域
 *
 * 自定义属性（attrs.xml）：
 * - sk_skeletonColor：骨架块背景色（默认 #E0E0E0）
 * - sk_highlightColor：闪烁高亮色（默认 #F5F5F5）
 * - sk_cornerRadius：骨架块圆角半径（默认 8dp）
 * - sk_duration：闪烁动画时长，毫秒（默认 1000）
 * - sk_autoStart：是否自动开始动画（默认 true）
 *
 * 用法示例：
 * ```xml
 * <com.ch.core.ui.skeleton.SkeletonLayout
 *     android:id="@+id/skeleton"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:sk_cornerRadius="8dp"
 *     app:sk_duration="1200" />
 * ```
 *
 * ```kotlin
 * // 代码中添加骨架块
 * skeleton.addSkeletonBlock(0.05f, 0.03f, 0.9f, 0.04f)  // 标题
 * skeleton.addSkeletonBlock(0.05f, 0.10f, 0.6f, 0.03f)  // 第一行
 * skeleton.addSkeletonBlock(0.05f, 0.16f, 0.75f, 0.03f) // 第二行
 *
 * // 控制动画
 * skeleton.startSkeleton()
 * skeleton.stopSkeleton()
 * ```
 *
 * 坐标说明：
 * - 所有坐标使用百分比（0.0 ~ 1.0），相对于控件宽高
 * - 自动适配不同屏幕尺寸
 */
class SkeletonLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * 骨架块数据（使用百分比坐标）
     *
     * @param leftRatio 左边缘比例（0.0 ~ 1.0）
     * @param topRatio 上边缘比例（0.0 ~ 1.0）
     * @param widthRatio 宽度比例（0.0 ~ 1.0）
     * @param heightRatio 高度比例（0.0 ~ 1.0）
     */
    data class SkeletonBlock(
        val leftRatio: Float,
        val topRatio: Float,
        val widthRatio: Float,
        val heightRatio: Float
    )

    /** 骨架块背景色 */
    private var skeletonColor: Int = Color.parseColor("#E0E0E0")

    /** 闪烁高亮色 */
    private var highlightColor: Int = Color.parseColor("#F5F5F5")

    /** 骨架块圆角半径（px） */
    private var cornerRadiusPx: Float = dp2px(8f)

    /** 动画时长（ms） */
    private var durationMs: Long = 1000L

    /** 是否自动开始 */
    private var autoStart: Boolean = true

    /** 骨架块列表 */
    private val blocks = mutableListOf<SkeletonBlock>()

    /** 绘制用矩形 */
    private val rectF = RectF()

    /** 骨架块画笔 */
    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = skeletonColor
        style = Paint.Style.FILL
    }

    /** 渐变高亮画笔 */
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** 闪烁动画器 */
    private var animator: ValueAnimator? = null

    /** 当前动画进度（0.0 ~ 1.0） */
    private var animProgress: Float = 0f

    /** 是否正在运行动画 */
    private var isRunning: Boolean = false

    init {
        // 解析自定义属性
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.SkeletonLayout)
            skeletonColor = ta.getColor(
                R.styleable.SkeletonLayout_sk_skeletonColor,
                skeletonColor
            )
            highlightColor = ta.getColor(
                R.styleable.SkeletonLayout_sk_highlightColor,
                highlightColor
            )
            cornerRadiusPx = ta.getDimension(
                R.styleable.SkeletonLayout_sk_cornerRadius,
                cornerRadiusPx
            )
            durationMs = ta.getInt(
                R.styleable.SkeletonLayout_sk_duration,
                1000
            ).toLong()
            autoStart = ta.getBoolean(
                R.styleable.SkeletonLayout_sk_autoStart,
                true
            )
            ta.recycle()
        }

        blockPaint.color = skeletonColor

        // 添加默认骨架块（模拟常见列表布局）
        addDefaultBlocks()

        if (autoStart) {
            post { startSkeleton() }
        }
    }

    /**
     * 添加默认骨架块
     *
     * 模拟一个典型的列表项布局：
     * - 头像（正方形）
     * - 标题行
     * - 副标题行
     * - 内容行 1
     * - 内容行 2
     */
    private fun addDefaultBlocks() {
        // 头像（左侧正方形）
        addSkeletonBlock(0.05f, 0.05f, 0.12f, 0.12f)
        // 标题（右侧上方）
        addSkeletonBlock(0.20f, 0.05f, 0.50f, 0.04f)
        // 副标题
        addSkeletonBlock(0.20f, 0.11f, 0.35f, 0.03f)
        // 内容行 1
        addSkeletonBlock(0.05f, 0.22f, 0.90f, 0.03f)
        // 内容行 2
        addSkeletonBlock(0.05f, 0.28f, 0.75f, 0.03f)
        // 内容行 3
        addSkeletonBlock(0.05f, 0.34f, 0.85f, 0.03f)
    }

    /**
     * 添加自定义骨架块
     *
     * 所有参数均为百分比（0.0 ~ 1.0），相对于控件宽高。
     *
     * @param leftRatio 左边缘比例
     * @param topRatio 上边缘比例
     * @param widthRatio 宽度比例
     * @param heightRatio 高度比例
     */
    fun addSkeletonBlock(
        leftRatio: Float,
        topRatio: Float,
        widthRatio: Float,
        heightRatio: Float
    ) {
        blocks.add(SkeletonBlock(leftRatio, topRatio, widthRatio, heightRatio))
    }

    /**
     * 清空所有骨架块
     */
    fun clearBlocks() {
        blocks.clear()
        invalidate()
    }

    /**
     * 开始骨架屏动画
     *
     * 启动从左到右的渐变扫光效果，循环播放。
     */
    fun startSkeleton() {
        if (isRunning) return
        isRunning = true
        visibility = VISIBLE

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * 停止骨架屏动画
     *
     * 停止动画并将控件隐藏。
     */
    fun stopSkeleton() {
        isRunning = false
        animator?.cancel()
        animator = null
        visibility = GONE
    }

    /**
     * 是否正在运行动画
     */
    fun isSkeletonRunning(): Boolean = isRunning

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f || blocks.isEmpty()) return

        // 绘制骨架块
        blockPaint.color = skeletonColor
        for (block in blocks) {
            rectF.set(
                block.leftRatio * w,
                block.topRatio * h,
                (block.leftRatio + block.widthRatio) * w,
                (block.topRatio + block.heightRatio) * h
            )
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, blockPaint)
        }

        // 绘制渐变扫光效果
        val shimmerWidth = w * 0.3f
        val shimmerCenter = animProgress * (w + shimmerWidth) - shimmerWidth / 2f

        val gradient = LinearGradient(
            shimmerCenter - shimmerWidth / 2f, 0f,
            shimmerCenter + shimmerWidth / 2f, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                highlightColor,
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        shimmerPaint.shader = gradient

        for (block in blocks) {
            rectF.set(
                block.leftRatio * w,
                block.topRatio * h,
                (block.leftRatio + block.widthRatio) * w,
                (block.topRatio + block.heightRatio) * h
            )
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, shimmerPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSkeleton()
    }

    /**
     * dp 转 px
     */
    private fun dp2px(dp: Float): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }
}
