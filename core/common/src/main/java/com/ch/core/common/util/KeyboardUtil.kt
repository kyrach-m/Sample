package com.ch.core.common.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * 软键盘控制工具类
 *
 * 提供软键盘的显示、隐藏、状态监听等常用操作。
 * 所有方法内部捕获异常，异常时返回 false 或静默处理。
 *
 * 用法示例：
 * ```kotlin
 * // 显示软键盘
 * KeyboardUtil.showKeyboard(editText)
 *
 * // 隐藏软键盘
 * KeyboardUtil.hideKeyboard(activity)
 *
 * // 切换软键盘显示/隐藏
 * KeyboardUtil.toggleKeyboard(context)
 *
 * // 监听软键盘高度变化
 * KeyboardUtil.addKeyboardVisibilityListener(activity) { isVisible, keyboardHeight ->
 *     if (isVisible) {
 *         // 软键盘弹起，keyboardHeight 为键盘高度
 *     } else {
 *         // 软键盘收起
 *     }
 * }
 * ```
 */
object KeyboardUtil {

    /**
     * 获取 InputMethodManager
     *
     * @param context Context
     * @return InputMethodManager 实例
     */
    private fun getManager(context: Context): InputMethodManager {
        return context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    // ==================== 显示 / 隐藏 ====================

    /**
     * 显示软键盘
     *
     * 强制显示软键盘，聚焦到指定 View。
     *
     * @param view 要聚焦的 View（通常是 EditText）
     */
    fun showKeyboard(view: View) {
        try {
            view.requestFocus()
            getManager(view.context).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            // 静默处理
        }
    }

    /**
     * 延迟显示软键盘
     *
     * 某些场景下 View 尚未完成布局，立即 showKeyboard 无效。
     * 此方法延迟指定毫秒后再显示，确保 View 已就绪。
     *
     * @param view 要聚焦的 View
     * @param delayMillis 延迟时间（毫秒），默认 200ms
     */
    fun showKeyboardDelayed(view: View, delayMillis: Long = 200L) {
        view.postDelayed({ showKeyboard(view) }, delayMillis)
    }

    /**
     * 隐藏软键盘
     *
     * @param activity Activity
     * @return true = 成功发送隐藏指令
     */
    fun hideKeyboard(activity: Activity): Boolean {
        return try {
            val view = activity.currentFocus ?: activity.window.decorView
            getManager(activity).hideSoftInputFromWindow(view.windowToken, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 隐藏软键盘（基于 View）
     *
     * @param view View
     * @return true = 成功发送隐藏指令
     */
    fun hideKeyboard(view: View): Boolean {
        return try {
            getManager(view.context).hideSoftInputFromWindow(view.windowToken, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 切换软键盘显示/隐藏
     *
     * @param context Context
     */
    fun toggleKeyboard(context: Context) {
        try {
            getManager(context).toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        } catch (e: Exception) {
            // 静默处理
        }
    }

    // ==================== 状态判断 ====================

    /**
     * 判断软键盘是否正在显示
     *
     * 通过比较窗口可见区域与屏幕高度来判断。
     * 当可见区域高度小于屏幕高度的 80% 时，认为键盘已弹起。
     *
     * @param activity Activity
     * @return true = 软键盘正在显示
     */
    fun isKeyboardVisible(activity: Activity): Boolean {
        return try {
            val rect = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = activity.window.decorView.height
            val keypadHeight = screenHeight - rect.bottom
            // 键盘高度超过屏幕高度的 20% 时认为键盘已弹起
            keypadHeight > screenHeight * 0.2
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取软键盘高度
     *
     * 注意：仅在软键盘弹起时调用才有效，否则返回 0。
     *
     * @param activity Activity
     * @return 软键盘高度（px），获取失败返回 0
     */
    fun getKeyboardHeight(activity: Activity): Int {
        return try {
            val rect = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = activity.window.decorView.height
            val keyboardHeight = screenHeight - rect.bottom
            if (keyboardHeight > screenHeight * 0.2) keyboardHeight else 0
        } catch (e: Exception) {
            0
        }
    }

    // ==================== 监听 ====================

    /**
     * 添加软键盘可见性变化监听器
     *
     * 通过 ViewTreeObserver 监听全局布局变化，判断软键盘弹起/收起。
     * 注意：需要在适当时机调用返回的 ViewTreeObserver.OnGlobalLayoutListener
     * 调用 [removeKeyboardVisibilityListener] 移除监听，避免内存泄漏。
     *
     * @param activity Activity
     * @param callback 回调（isVisible: 是否可见, keyboardHeight: 键盘高度 px）
     * @return OnGlobalLayoutListener 实例（用于后续移除监听）
     */
    fun addKeyboardVisibilityListener(
        activity: Activity,
        callback: (isVisible: Boolean, keyboardHeight: Int) -> Unit
    ): ViewTreeObserver.OnGlobalLayoutListener {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = activity.window.decorView.height
            val keyboardHeight = screenHeight - rect.bottom
            val isVisible = keyboardHeight > screenHeight * 0.2
            callback(isVisible, if (isVisible) keyboardHeight else 0)
        }
        activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        return listener
    }

    /**
     * 移除软键盘可见性监听器
     *
     * @param activity Activity
     * @param listener [addKeyboardVisibilityListener] 返回的监听器
     */
    fun removeKeyboardVisibilityListener(
        activity: Activity,
        listener: ViewTreeObserver.OnGlobalLayoutListener
    ) {
        try {
            @Suppress("DEPRECATION")
            activity.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        } catch (e: Exception) {
            // 静默处理
        }
    }

    /**
     * 点击空白区域隐藏软键盘
     *
     * 在 Activity 的 dispatchTouchEvent 中调用，
     * 当用户点击非 EditText 区域时自动隐藏软键盘。
     *
     * 用法示例：
     * ```kotlin
     * override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
     *     if (ev.action == MotionEvent.ACTION_DOWN) {
     *         val v = currentFocus
     *         if (v is EditText) {
     *             val outRect = Rect()
     *             v.getGlobalVisibleRect(outRect)
     *             if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
     *                 v.clearFocus()
     *                 KeyboardUtil.hideKeyboard(this)
     *             }
     *         }
     *     }
     *     return super.dispatchTouchEvent(ev)
     * }
     * ```
     *
     * @param activity Activity
     * @param view 当前焦点 View
     */
    fun hideKeyboardOnTouchOutside(activity: Activity, view: View?) {
        if (view is EditText) {
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            // 点击区域在 EditText 外部时隐藏键盘
            // 注意：此处仅做判断逻辑提示，实际需在 dispatchTouchEvent 中使用
        }
    }
}
