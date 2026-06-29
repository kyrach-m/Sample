package com.ch.core.ui.state

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.ch.core.ui.R

/**
 * 多状态布局管理器
 *
 * 功能特性：
 * - 管理 4 种状态视图：Loading、Content、Empty、Error
 * - 通过 [setState] 方法切换状态，自动显示/隐藏对应视图
 * - Error 状态支持点击重试回调
 * - 支持通过 XML 自定义属性或代码动态设置各状态布局
 * - Content 视图为子 View（通过 XML 或 addView 添加）
 *
 * 自定义属性（attrs.xml）：
 * - sl_loadingLayout：Loading 状态布局引用
 * - sl_emptyLayout：Empty 状态布局引用
 * - sl_errorLayout：Error 状态布局引用
 * - sl_emptyIcon：空状态图标
 * - sl_emptyText：空状态文字
 * - sl_errorIcon：错误状态图标
 * - sl_errorText：错误状态文字
 * - sl_initialState：初始状态（loading/content/empty/error）
 *
 * 用法示例：
 * ```xml
 * <com.ch.core.ui.state.StateLayout
 *     android:id="@+id/stateLayout"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:sl_emptyText="暂无数据"
 *     app:sl_errorText="网络异常">
 *
 *     <!-- Content 视图 -->
 *     <androidx.recyclerview.widget.RecyclerView
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" />
 *
 * </com.ch.core.ui.state.StateLayout>
 * ```
 *
 * ```kotlin
 * // 切换状态
 * stateLayout.setState(StateLayout.State.LOADING)
 * stateLayout.setState(StateLayout.State.CONTENT)
 * stateLayout.setState(StateLayout.State.EMPTY)
 * stateLayout.setState(StateLayout.State.ERROR)
 *
 * // 设置重试回调
 * stateLayout.onRetry = { loadData() }
 * ```
 */
class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * 状态枚举
     *
     * @property LOADING 加载中
     * @property CONTENT 内容展示
     * @property EMPTY 空数据
     * @property ERROR 加载错误
     */
    enum class State {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    /** 当前状态 */
    private var currentState: State = State.CONTENT

    /** 重试回调 */
    var onRetry: (() -> Unit)? = null

    /** 各状态视图 */
    private var loadingView: View? = null
    private var emptyView: View? = null
    private var errorView: View? = null

    /** 自定义属性值 */
    private var emptyTextAttr: String? = null
    private var errorTextAttr: String? = null
    private var emptyIconAttr: Int = 0
    private var errorIconAttr: Int = 0
    private var initialState: Int = 1  // 默认 content

    init {
        // 解析自定义属性
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.StateLayout)

            val loadingLayoutRes = ta.getResourceId(
                R.styleable.StateLayout_sl_loadingLayout,
                R.layout.layout_state_loading
            )
            val emptyLayoutRes = ta.getResourceId(
                R.styleable.StateLayout_sl_emptyLayout,
                R.layout.layout_state_empty
            )
            val errorLayoutRes = ta.getResourceId(
                R.styleable.StateLayout_sl_errorLayout,
                R.layout.layout_state_error
            )

            emptyTextAttr = ta.getString(R.styleable.StateLayout_sl_emptyText)
            errorTextAttr = ta.getString(R.styleable.StateLayout_sl_errorText)
            emptyIconAttr = ta.getResourceId(R.styleable.StateLayout_sl_emptyIcon, 0)
            errorIconAttr = ta.getResourceId(R.styleable.StateLayout_sl_errorIcon, 0)
            initialState = ta.getInt(R.styleable.StateLayout_sl_initialState, 1)

            ta.recycle()

            // 填充各状态视图
            loadingView = LayoutInflater.from(context)
                .inflate(loadingLayoutRes, this, false)
            emptyView = LayoutInflater.from(context)
                .inflate(emptyLayoutRes, this, false)
            errorView = LayoutInflater.from(context)
                .inflate(errorLayoutRes, this, false)

            // 应用自定义文字和图标
            applyCustomAttributes()

            // 添加状态视图（顺序：loading、empty、error，content 由子 View 提供）
            addView(loadingView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            addView(emptyView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            addView(errorView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

            // 设置 Error 视图点击重试
            errorView?.let { ev ->
                val retryView = ev.findViewById<TextView>(R.id.tv_retry)
                retryView?.setOnClickListener {
                    onRetry?.invoke()
                }
                // 整个 error 视图也可点击重试
                ev.setOnClickListener {
                    onRetry?.invoke()
                }
            }
        } ?: run {
            // 无 attrs 时也要创建默认状态视图
            loadingView = LayoutInflater.from(context)
                .inflate(R.layout.layout_state_loading, this, false)
            emptyView = LayoutInflater.from(context)
                .inflate(R.layout.layout_state_empty, this, false)
            errorView = LayoutInflater.from(context)
                .inflate(R.layout.layout_state_error, this, false)

            addView(loadingView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            addView(emptyView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            addView(errorView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

            errorView?.findViewById<TextView>(R.id.tv_retry)?.setOnClickListener {
                onRetry?.invoke()
            }
            errorView?.setOnClickListener {
                onRetry?.invoke()
            }
        }

        // 设置初始状态
        currentState = when (initialState) {
            0 -> State.LOADING
            1 -> State.CONTENT
            2 -> State.EMPTY
            3 -> State.ERROR
            else -> State.CONTENT
        }
        updateVisibility()
    }

    /**
     * 设置当前状态
     *
     * 根据状态自动切换显示对应视图：
     * - [State.LOADING]：显示 Loading 视图
     * - [State.CONTENT]：显示内容子 View（除状态视图外的所有子 View）
     * - [State.EMPTY]：显示空状态视图
     * - [State.ERROR]：显示错误状态视图
     *
     * @param state 目标状态
     */
    fun setState(state: State) {
        if (currentState == state) return
        currentState = state
        updateVisibility()
    }

    /**
     * 获取当前状态
     */
    fun getState(): State = currentState

    /**
     * 设置空状态文字
     *
     * @param text 空状态提示文字
     */
    fun setEmptyText(text: String) {
        emptyView?.findViewById<TextView>(R.id.tv_empty_text)?.text = text
    }

    /**
     * 设置错误状态文字
     *
     * @param text 错误状态提示文字
     */
    fun setErrorText(text: String) {
        errorView?.findViewById<TextView>(R.id.tv_error_text)?.text = text
    }

    /**
     * 更新各视图可见性
     */
    private fun updateVisibility() {
        loadingView?.visibility = if (currentState == State.LOADING) VISIBLE else GONE
        emptyView?.visibility = if (currentState == State.EMPTY) VISIBLE else GONE
        errorView?.visibility = if (currentState == State.ERROR) VISIBLE else GONE

        // Content 视图：除状态视图外的所有子 View
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== loadingView && child !== emptyView && child !== errorView) {
                child.visibility = if (currentState == State.CONTENT) VISIBLE else GONE
            }
        }
    }

    /**
     * 应用 XML 自定义属性中的文字和图标
     */
    private fun applyCustomAttributes() {
        emptyTextAttr?.let { text ->
            emptyView?.findViewById<TextView>(R.id.tv_empty_text)?.text = text
        }
        errorTextAttr?.let { text ->
            errorView?.findViewById<TextView>(R.id.tv_error_text)?.text = text
        }
        if (emptyIconAttr != 0) {
            emptyView?.findViewById<ImageView>(R.id.iv_empty_icon)?.setImageResource(emptyIconAttr)
        }
        if (errorIconAttr != 0) {
            errorView?.findViewById<ImageView>(R.id.iv_error_icon)?.setImageResource(errorIconAttr)
        }
    }
}
