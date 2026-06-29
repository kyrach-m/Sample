package com.ch.core.ui.veil

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.ch.core.ui.R

/**
 * RecyclerView 骨架屏容器
 *
 * 参考 AndroidVeil 库设计，在 RecyclerView 上层覆盖骨架屏占位布局。
 * 骨架屏基于列表项布局渲染，支持微光（Shimmer）扫光动画。
 *
 * 功能特性：
 * - 内部包含 RecyclerView + 骨架屏覆盖层
 * - setVeilLayout() 设置列表项骨架屏布局
 * - showVeil() / hideVeil() 控制骨架屏显隐
 * - 微光扫光动画效果
 * - 支持自定义 baseColor、highlightColor、cornerRadius
 *
 * 自定义属性（attrs.xml）：
 * - vrf_baseColor：骨架块基础色（默认 #E0E0E0）
 * - vrf_highlightColor：高亮色（默认 #F5F5F5）
 * - vrf_cornerRadius：圆角半径（默认 8dp）
 * - vrf_shimmerDuration：动画时长 ms（默认 1000）
 * - vrf_itemCount：骨架项数量（默认 6）
 *
 * 用法示例：
 * ```xml
 * <com.ch.core.ui.veil.VeilRecyclerFrameView
 *     android:id="@+id/veilFrame"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:vrf_itemCount="8"
 *     app:vrf_cornerRadius="8dp" />
 * ```
 *
 * ```kotlin
 * // 设置列表项骨架布局
 * veilFrame.setVeilLayout(R.layout.item_skeleton)
 *
 * // 显示骨架屏
 * veilFrame.showVeil()
 *
 * // 数据加载完成后隐藏
 * veilFrame.hideVeil()
 * ```
 */
class VeilRecyclerFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** RecyclerView */
    val recyclerView: RecyclerView

    /** 骨架屏覆盖层 */
    private val veilView: VeilOverlayView

    /** 骨架项布局资源 ID */
    @LayoutRes
    private var veilLayoutRes: Int = 0

    /** 骨架项数量 */
    private var itemCount: Int = 6

    /** 基础色 */
    private var baseColor: Int = Color.parseColor("#E0E0E0")

    /** 高亮色 */
    private var highlightColor: Int = Color.parseColor("#F5F5F5")

    /** 圆角 */
    private var cornerRadiusPx: Float = dp2px(8f)

    /** 动画时长 */
    private var shimmerDuration: Long = 1000L

    init {
        // 解析自定义属性
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.VeilRecyclerFrameView)
            baseColor = ta.getColor(R.styleable.VeilRecyclerFrameView_vrf_baseColor, baseColor)
            highlightColor = ta.getColor(R.styleable.VeilRecyclerFrameView_vrf_highlightColor, highlightColor)
            cornerRadiusPx = ta.getDimension(R.styleable.VeilRecyclerFrameView_vrf_cornerRadius, cornerRadiusPx)
            shimmerDuration = ta.getInt(R.styleable.VeilRecyclerFrameView_vrf_shimmerDuration, 1000).toLong()
            itemCount = ta.getInt(R.styleable.VeilRecyclerFrameView_vrf_itemCount, 6)
            ta.recycle()
        }

        // 创建 RecyclerView
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            overScrollMode = OVER_SCROLL_NEVER
        }
        addView(recyclerView)

        // 创建骨架屏覆盖层
        veilView = VeilOverlayView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = GONE
        }
        addView(veilView)
    }

    /**
     * 设置列表项骨架屏布局
     *
     * 骨架屏将根据此布局渲染多个列表项。
     *
     * @param layoutId 列表项骨架布局资源 ID
     */
    fun setVeilLayout(@LayoutRes layoutId: Int) {
        veilLayoutRes = layoutId
    }

    /**
     * 显示骨架屏
     *
     * 骨架屏覆盖在 RecyclerView 上方，显示微光动画。
     */
    fun showVeil() {
        if (veilLayoutRes != 0) {
            veilView.inflateVeilLayout(veilLayoutRes, itemCount)
        }
        veilView.setShimmerConfig(baseColor, highlightColor, cornerRadiusPx, shimmerDuration)
        veilView.visibility = VISIBLE
        veilView.startShimmer()
    }

    /**
     * 隐藏骨架屏
     */
    fun hideVeil() {
        veilView.stopShimmer()
        veilView.visibility = GONE
    }

    /**
     * 是否正在显示骨架屏
     */
    fun isVeilVisible(): Boolean = veilView.visibility == VISIBLE

    /**
     * 设置骨架项数量
     *
     * @param count 骨架项数量
     */
    fun setItemCount(count: Int) {
        itemCount = count
    }

    /**
     * 骨架屏覆盖层 View
     *
     * 内部通过 LayoutInflater 填充列表项骨架布局，
     * 并在其上绘制微光扫光效果。
     */
    private class VeilOverlayView(context: Context) : FrameLayout(context) {

        /** 骨架块矩形列表（百分比坐标） */
        private val blocks = mutableListOf<RectF>()

        /** 骨架块绘制画笔 */
        private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        /** 微光画笔 */
        private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        /** 动画进度 */
        private var animProgress: Float = 0f

        /** 动画器 */
        private var animator: ValueAnimator? = null

        /** 基础色 */
        private var baseColor: Int = Color.parseColor("#E0E0E0")

        /** 高亮色 */
        private var highlightColor: Int = Color.parseColor("#F5F5F5")

        /** 圆角 */
        private var cornerRadius: Float = dp2px(8f)

        /** 动画时长 */
        private var shimmerDuration: Long = 1000L

        /**
         * 填充骨架布局
         *
         * 将布局文件 inflate 后，提取其中所有子 View 的位置信息作为骨架块。
         *
         * @param layoutRes 骨架布局资源
         * @param count 重复次数（列表项数量）
         */
        fun inflateVeilLayout(@LayoutRes layoutRes: Int, count: Int) {
            removeAllViews()
            blocks.clear()

            // 简单策略：将每个骨架项等分为若干水平条
            // 实际使用时建议通过 setVeilLayout 设置布局，这里用默认条
            val itemHeightRatio = 1f / count
            for (i in 0 until count) {
                val topRatio = i * itemHeightRatio
                // 每个 item 内添加 3 行骨架条
                addBlock(0.05f, topRatio + itemHeightRatio * 0.15f, 0.90f, itemHeightRatio * 0.18f)
                addBlock(0.05f, topRatio + itemHeightRatio * 0.45f, 0.60f, itemHeightRatio * 0.15f)
                addBlock(0.05f, topRatio + itemHeightRatio * 0.70f, 0.75f, itemHeightRatio * 0.15f)
            }
        }

        /**
         * 添加骨架块
         */
        private fun addBlock(leftRatio: Float, topRatio: Float, widthRatio: Float, heightRatio: Float) {
            // 存储百分比，onDraw 时转换为实际坐标
            blocks.add(RectF(leftRatio, topRatio, leftRatio + widthRatio, topRatio + heightRatio))
        }

        /**
         * 设置微光配置
         */
        fun setShimmerConfig(base: Int, highlight: Int, radius: Float, duration: Long) {
            baseColor = base
            highlightColor = highlight
            cornerRadius = radius
            shimmerDuration = duration
            blockPaint.color = baseColor
        }

        /**
         * 开始微光动画
         */
        fun startShimmer() {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = shimmerDuration
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener {
                    animProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        /**
         * 停止微光动画
         */
        fun stopShimmer() {
            animator?.cancel()
            animator = null
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w == 0f || h == 0f || blocks.isEmpty()) return

            // 绘制骨架块
            blockPaint.color = baseColor
            val rect = RectF()
            for (block in blocks) {
                rect.set(
                    block.left * w,
                    block.top * h,
                    block.right * w,
                    block.bottom * h
                )
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, blockPaint)
            }

            // 绘制微光扫光
            val shimmerWidth = w * 0.3f
            val shimmerCenter = animProgress * (w + shimmerWidth) - shimmerWidth / 2f
            val gradient = LinearGradient(
                shimmerCenter - shimmerWidth / 2f, 0f,
                shimmerCenter + shimmerWidth / 2f, 0f,
                intArrayOf(Color.TRANSPARENT, highlightColor, Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            shimmerPaint.shader = gradient
            for (block in blocks) {
                rect.set(
                    block.left * w,
                    block.top * h,
                    block.right * w,
                    block.bottom * h
                )
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shimmerPaint)
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            stopShimmer()
        }

        private fun dp2px(dp: Float): Float = dp * context.resources.displayMetrics.density
    }

    private fun dp2px(dp: Float): Float = dp * context.resources.displayMetrics.density
}
