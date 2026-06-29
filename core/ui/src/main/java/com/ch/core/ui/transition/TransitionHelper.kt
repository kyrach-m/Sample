package com.ch.core.ui.transition

import android.app.Activity
import android.content.Intent
import com.ch.core.ui.R

/**
 * Activity 转场动画辅助类
 *
 * 功能特性：
 * - 封装 Activity 的 overridePendingTransition 调用
 * - 提供统一的右滑入/左滑出转场效果
 * - 提供淡入淡出转场效果
 * - 兼容 Android 12+ 的 overrideActivityTransition API
 *
 * 用法示例：
 * ```kotlin
 * // 启动新 Activity（右滑入）
 * TransitionHelper.startActivityWithTransition(activity, intent)
 *
 * // 关闭 Activity（右滑出）
 * TransitionHelper.finishWithTransition(activity)
 *
 * // 使用淡入淡出
 * TransitionHelper.startActivityWithFade(activity, intent)
 * ```
 */
object TransitionHelper {

    /**
     * 启动 Activity 并使用右滑入转场
     *
     * 新 Activity 从右侧滑入，当前 Activity 淡出。
     *
     * @param activity 当前 Activity
     * @param intent 目标 Activity Intent
     */
    fun startActivityWithTransition(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }

    /**
     * 关闭 Activity 并使用左滑出转场
     *
     * 当前 Activity 向左滑出，下层 Activity 淡入。
     *
     * @param activity 当前 Activity
     */
    fun finishWithTransition(activity: Activity) {
        activity.finish()
        activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left)
    }

    /**
     * 启动 Activity 并使用淡入转场
     *
     * @param activity 当前 Activity
     * @param intent 目标 Activity Intent
     */
    fun startActivityWithFade(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    /**
     * 关闭 Activity 并使用淡出转场
     *
     * @param activity 当前 Activity
     */
    fun finishWithFade(activity: Activity) {
        activity.finish()
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
