package com.ch.core.ui.loading

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.ch.core.ui.R

/**
 * 全屏半透明黑色遮罩加载控件
 *
 * 功能特性：
 * - 半透明黑色背景遮罩，覆盖整个父容器
 * - 中央显示 ProgressBar + 文字提示
 * - 显示时拦截所有触摸事件，防止穿透
 * - dismiss() 带有 200ms 渐隐动画
 * - 支持通过 attrs.xml 自定义属性配置样式
 *
 * 自定义属性（attrs.xml）：
 * - lm_maskColor：遮罩背景颜色（默认 #99000000）
 * - lm_textColor：加载文字颜色（默认白色）
 * - lm_textSize：加载文字大小（默认 14sp）
 * - lm_defaultText：默认显示文字（默认"加载中…"）
 *
 * 用法示例：
 * ```xml
 * <!-- 在布局中使用 -->
 * <com.ch.core.ui.loading.LoadingMask
 *     android:id="@+id/loadingMask"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:lm_maskColor="#CC000000"
 *     app:lm_textColor="#FFFFFF"
 *     app:lm_defaultText="请稍候…" />
 * ```
 *
 * ```kotlin
 * // 代码中使用
 * loadingMask.show("上传中…")
 * loadingMask.dismiss()
 * ```
 *
 * 注意：
 * - 该控件默认 GONE，调用 show() 后变为 VISIBLE
 * - 建议在 XML 中将其作为根布局或 DecorView 的子 View
 */
class LoadingMask @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 遮罩背景颜色 */
    private var maskColor: Int = Color.parseColor("#99000000")

    /** 加载文字颜色 */
    private var textColor: Int = Color.WHITE

    /** 加载文字大小（px） */
    private var textSizePx: Float = sp2px(14f)

    /** 默认显示文字 */
    private var defaultText: String = ""

    /** 中央内容容器 */
    private val contentView: View

    /** 进度条 */
    private val progressBar: ProgressBar

    /** 文字提示 */
    private val textView: TextView

    init {
        // 解析自定义属性
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.LoadingMask)
            maskColor = ta.getColor(R.styleable.LoadingMask_lm_maskColor, maskColor)
            textColor = ta.getColor(R.styleable.LoadingMask_lm_textColor, textColor)
            textSizePx = ta.getDimension(
                R.styleable.LoadingMask_lm_textSize,
                textSizePx
            )
            defaultText = ta.getString(R.styleable.LoadingMask_lm_defaultText)
                ?: context.getString(R.string.core_ui_loading)
            ta.recycle()
        }

        // 设置遮罩背景
        setBackgroundColor(maskColor)

        // 填充中央内容布局
        contentView = LayoutInflater.from(context)
            .inflate(R.layout.layout_loading_mask_content, this, false)
        progressBar = contentView.findViewById(R.id.pb_loading)
        textView = contentView.findViewById(R.id.tv_loading_text)

        // 应用文字样式
        textView.setTextColor(textColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        textView.text = defaultText

        // 居中添加到遮罩层
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.gravity = android.view.Gravity.CENTER
        addView(contentView, lp)

        // 默认隐藏
        visibility = GONE

        // 拦截所有触摸事件
        isClickable = true
        isFocusable = true
    }

    /**
     * 显示加载遮罩
     *
     * @param text 加载提示文字，默认使用 attrs 中配置的 lm_defaultText
     */
    fun show(text: String = defaultText) {
        textView.text = text
        // 取消正在进行的动画（防止 dismiss 动画未完成）
        animate().cancel()
        alpha = 1f
        visibility = VISIBLE
    }

    /**
     * 显示带进度的加载遮罩
     *
     * @param progress 进度值（0-100）
     * @param message 加载提示文字（可选）
     */
    fun showWithProgress(progress: Int, message: String? = null) {
        show(message ?: defaultText)
        progressBar.isIndeterminate = false
        progressBar.progress = progress.coerceIn(0, 100)
    }

    /**
     * 更新加载进度
     *
     * @param progress 进度值（0-100）
     */
    fun updateProgress(progress: Int) {
        progressBar.progress = progress.coerceIn(0, 100)
    }

    /**
     * 更新加载文案
     *
     * @param message 新的加载提示文字
     */
    fun setMessage(message: String) {
        textView.text = message
    }

    /**
     * 隐藏加载遮罩（带 200ms 渐隐动画）
     *
     * 动画完成后将 visibility 设为 GONE，释放绘制资源。
     */
    fun dismiss() {
        if (visibility != VISIBLE) return
        animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { visibility = GONE }
            .start()
    }

    /**
     * 拦截所有触摸事件，防止穿透到底层 View
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    /**
     * sp 转 px
     */
    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
    }
}
