package com.ch.core.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ch.core.ui.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 统一样式底部弹出对话框
 *
 * 功能特性：
 * - 顶部圆角（radius_l = 16dp）
 * - 支持设置高度比例
 * - 支持设置是否可拖拽关闭
 * - 支持设置自定义内容视图
 * - 自动适配深色模式
 *
 * 用法示例：
 * ```kotlin
 * class MyBottomSheet : CoreBottomSheetDialog() {
 *     override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View {
 *         return inflater.inflate(R.layout.my_bottom_sheet, container, false)
 *     }
 * }
 *
 * // 显示
 * MyBottomSheet().show(supportFragmentManager, "tag")
 * ```
 *
 * 或者使用静态工厂方法：
 * ```kotlin
 * CoreBottomSheetDialog.newInstance(R.layout.my_bottom_sheet)
 *     .setPeekHeight(600)
 *     .setCancelable(true)
 *     .show(supportFragmentManager, "tag")
 * ```
 */
open class CoreBottomSheetDialog : BottomSheetDialogFragment() {

    /** 内容布局 ID */
    private var contentLayoutRes: Int = 0

    /** 弹窗高度（px，0 表示自适应） */
    private var peekHeightPx: Int = 0

    /** 是否可拖拽关闭 */
    private var draggable: Boolean = true

    override fun getTheme(): Int = R.style.CoreBottomSheetStyle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (contentLayoutRes != 0) {
            inflater.inflate(contentLayoutRes, container, false)
        } else {
            onCreateContentView(inflater, container)
        }
    }

    /**
     * 子类重写此方法提供自定义内容视图
     *
     * @param inflater 布局填充器
     * @param container 父容器
     * @return 内容 View
     */
    protected open fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View? = null

    override fun onStart() {
        super.onStart()
        dialog?.let { d ->
            if (peekHeightPx > 0) {
                val bottomSheet = d.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )
                bottomSheet?.layoutParams?.height = peekHeightPx
                bottomSheet?.requestLayout()
            }
        }
        isCancelable = draggable
    }

    /**
     * 设置内容布局资源
     *
     * @param layoutRes 布局资源 ID
     * @return this
     */
    fun setContentLayout(layoutRes: Int): CoreBottomSheetDialog {
        this.contentLayoutRes = layoutRes
        return this
    }

    /**
     * 设置弹窗高度
     *
     * @param height 高度（px）
     * @return this
     */
    fun setPeekHeight(height: Int): CoreBottomSheetDialog {
        this.peekHeightPx = height
        return this
    }

    /**
     * 设置是否可拖拽关闭
     *
     * @param draggable 是否可拖拽
     * @return this
     */
    fun setDraggable(draggable: Boolean): CoreBottomSheetDialog {
        this.draggable = draggable
        return this
    }

    companion object {
        /**
         * 创建实例（指定内容布局）
         *
         * @param layoutRes 布局资源 ID
         * @return CoreBottomSheetDialog 实例
         */
        fun newInstance(layoutRes: Int): CoreBottomSheetDialog {
            return CoreBottomSheetDialog().apply {
                contentLayoutRes = layoutRes
            }
        }
    }
}
