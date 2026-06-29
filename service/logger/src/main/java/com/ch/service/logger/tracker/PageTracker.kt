package com.ch.service.logger.tracker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ch.core.common.logger.Logger
import com.ch.service.logger.AnalyticsHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * 页面自动埋点追踪器
 *
 * 实现 [Application.ActivityLifecycleCallbacks]，自动监听所有 Activity 的生命周期，
 * 实现页面曝光（page_view）和离开（page_leave）的自动埋点。
 *
 * 核心特性：
 * - 自动上报 page_view 事件（Activity.onResume 时）
 * - 自动上报 page_leave 事件（Activity.onPause 时）
 * - 自动计算页面停留时长（onResume 到 onPause 的差值）
 * - 支持黑名单过滤（敏感页面不上报）
 * - 无需业务层手动埋点
 *
 * 使用示例：
 * ```kotlin
 * // 在 Application.onCreate 中注册
 * val pageTracker = PageTracker()
 * pageTracker.addToBlacklist("PaymentActivity") // 支付页面上报
 * registerActivityLifecycleCallbacks(pageTracker)
 * ```
 *
 * 事件参数说明：
 * - page_view：page_name（Activity 类名）、page_title（Activity 标题）
 * - page_leave：page_name、page_title、stay_duration（停留时长，毫秒）
 *
 * 黑名单说明：
 * - 黑名单中的页面不会触发 page_view / page_leave 事件
 * - 适用于支付页面、密码输入页面等敏感场景
 * - 通过 [addToBlacklist] / [removeFromBlacklist] 动态管理
 */
class PageTracker : Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "PageTracker"

        /**
         * 事件名称常量
         */
        private const val EVENT_PAGE_VIEW = "page_view"
        private const val EVENT_PAGE_LEAVE = "page_leave"
    }

    /**
     * 页面进入时间记录
     *
     * Key: Activity 的类名
     * Value: onResume 时的时间戳（毫秒）
     */
    private val resumeTimeMap = ConcurrentHashMap<String, Long>()

    /**
     * 黑名单页面列表
     *
     * 黑名单中的页面不会触发埋点事件。
     */
    private val blacklist = mutableSetOf<String>()

    /**
     * 添加页面到黑名单
     *
     * 黑名单中的页面不会触发 page_view / page_leave 事件。
     * 适用于敏感页面（如支付、密码输入）。
     *
     * @param activityName Activity 类名（如 "PaymentActivity"）
     */
    fun addToBlacklist(activityName: String) {
        blacklist.add(activityName)
        Logger.d(TAG, "已添加黑名单: $activityName")
    }

    /**
     * 从黑名单移除页面
     *
     * @param activityName Activity 类名
     */
    fun removeFromBlacklist(activityName: String) {
        blacklist.remove(activityName)
        Logger.d(TAG, "已移除黑名单: $activityName")
    }

    /**
     * Activity 进入前台（onResume）
     *
     * 自动上报 page_view 事件，参数：
     * - page_name：Activity 类名（simpleName）
     * - page_title：Activity 标题（title）
     *
     * 同时记录进入时间，用于计算停留时长。
     */
    override fun onActivityResumed(activity: Activity) {
        val pageName = activity.javaClass.simpleName

        // 黑名单检查
        if (pageName in blacklist) {
            Logger.d(TAG, "页面 $pageName 在黑名单中，跳过埋点")
            return
        }

        // 记录进入时间
        resumeTimeMap[pageName] = System.currentTimeMillis()

        // 获取页面标题
        val pageTitle = activity.title?.toString() ?: pageName

        // 上报 page_view 事件
        AnalyticsHelper.logEvent(
            EVENT_PAGE_VIEW,
            mapOf(
                "page_name" to pageName,
                "page_title" to pageTitle
            )
        )

        Logger.d(TAG, "page_view: $pageName ($pageTitle)")
    }

    /**
     * Activity 进入后台（onPause）
     *
     * 自动上报 page_leave 事件，参数：
     * - page_name：Activity 类名
     * - page_title：Activity 标题
     * - stay_duration：页面停留时长（毫秒）
     */
    override fun onActivityPaused(activity: Activity) {
        val pageName = activity.javaClass.simpleName

        // 黑名单检查
        if (pageName in blacklist) return

        // 计算停留时长
        val resumeTime = resumeTimeMap.remove(pageName)
        val stayDuration = if (resumeTime != null) {
            System.currentTimeMillis() - resumeTime
        } else {
            0L
        }

        val pageTitle = activity.title?.toString() ?: pageName

        // 上报 page_leave 事件
        AnalyticsHelper.logEvent(
            EVENT_PAGE_LEAVE,
            mapOf(
                "page_name" to pageName,
                "page_title" to pageTitle,
                "stay_duration" to stayDuration.toString()
            )
        )

        Logger.d(TAG, "page_leave: $pageName | 停留 ${stayDuration}ms")
    }

    // ========================================
    // 以下生命周期方法不需要处理
    // ========================================

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
