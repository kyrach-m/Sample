package com.ch.core.ui.widget.state

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.ch.core.ui.R

/**
 * 全局空状态视图
 *
 * 提供空数据、错误、无结果等状态的统一展示。
 *
 * 用法示例：
 * ```kotlin
 * emptyView.showEmpty("暂无数据", "点击重试") { }
 * emptyView.showError("加载失败", "重试") { }
 * emptyView.hide()
 * ```
 */
class GlobalEmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * 空状态类型
     */
    enum class EmptyType {
        /** 空数据 */
        EMPTY,
        /** 错误状态 */
        ERROR,
        /** 无结果 */
        NO_RESULT
    }

    private var titleTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private var retryButton: Button? = null

    init {
        visibility = GONE
    }

    /**
     * 显示空状态
     *
     * @param title 标题（可选）
     * @param subtitle 副标题（可选）
     * @param retryText 重试按钮文字（可选）
     * @param onRetry 重试回调（可选）
     */
    fun showEmpty(
        title: String? = null,
        subtitle: String? = null,
        retryText: String? = null,
        onRetry: (() -> Unit)? = null
    ) {
        showView(EmptyType.EMPTY, title, subtitle, retryText, onRetry)
    }

    /**
     * 显示错误状态
     *
     * @param title 标题（可选）
     * @param subtitle 副标题（可选）
     * @param retryText 重试按钮文字（可选）
     * @param onRetry 重试回调（可选）
     */
    fun showError(
        title: String? = null,
        subtitle: String? = null,
        retryText: String? = null,
        onRetry: (() -> Unit)? = null
    ) {
        showView(EmptyType.ERROR, title, subtitle, retryText, onRetry)
    }

    /**
     * 显示无结果状态
     *
     * @param title 标题（可选）
     * @param subtitle 副标题（可选）
     */
    fun showNoResult(
        title: String? = null,
        subtitle: String? = null
    ) {
        showView(EmptyType.NO_RESULT, title, subtitle, null, null)
    }

    /**
     * 隐藏空状态视图
     */
    fun hide() {
        visibility = GONE
    }

    private fun showView(
        type: EmptyType,
        title: String?,
        subtitle: String?,
        retryText: String?,
        onRetry: (() -> Unit)?
    ) {
        removeAllViews()

        val container = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        // 标题
        if (title != null) {
            titleTextView = TextView(context).apply {
                text = title
                textSize = 16f
                setPadding(0, 0, 0, 8)
                gravity = Gravity.CENTER
            }
        }

        // 副标题
        if (subtitle != null) {
            subtitleTextView = TextView(context).apply {
                text = subtitle
                textSize = 14f
                setPadding(0, 0, 0, 16)
                gravity = Gravity.CENTER
            }
        }

        // 重试按钮
        if (retryText != null && onRetry != null) {
            retryButton = Button(context).apply {
                text = retryText
                setOnClickListener { onRetry() }
            }
        }

        val innerContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER
            titleTextView?.let { addView(it) }
            subtitleTextView?.let { addView(it) }
            retryButton?.let { addView(it) }
        }

        container.addView(innerContainer)
        addView(container)
        visibility = VISIBLE
    }
}
