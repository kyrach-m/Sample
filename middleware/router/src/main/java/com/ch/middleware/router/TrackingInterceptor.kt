package com.ch.middleware.router

import android.content.Context
import com.ch.core.common.logger.Logger
import com.ch.middleware.router.annotation.RouteInterceptorDef
import com.ch.service.logger.AnalyticsHelper

/**
 * 全局埋点拦截器
 *
 * 基于 [RouteInterceptorV2] 实现，在每次路由跳转时自动上报页面跳转埋点事件。
 * 通过 [:service:logger] 模块的 [AnalyticsHelper] 记录事件，包含来源页面、目标页面和携带参数。
 *
 * **执行优先级**：priority = 2，在 [LoginInterceptor]（priority = 1）之后执行，
 * 确保只有实际放行的跳转才会触发埋点（被登录拦截的跳转不会上报）。
 *
 * **上报事件**：
 * - 事件名称：`page_navigation`
 * - 参数：
 *   - `target_path`：目标页面路由路径
 *   - `source_path`：来源页面路由路径（如有）
 *   - `params`：携带的参数字符串表示
 *
 * @see LoginInterceptor
 * @see AnalyticsHelper
 */
@RouteInterceptorDef(priority = 2)
class TrackingInterceptor : RouteInterceptorV2 {

    companion object {
        private const val TAG = "TrackingInterceptor"

        /**
         * 埋点事件名称：页面跳转
         */
        private const val EVENT_PAGE_NAVIGATION = "page_navigation"

        /**
         * 参数 Key：目标路径
         */
        private const val PARAM_TARGET_PATH = "target_path"

        /**
         * 参数 Key：来源路径
         */
        private const val PARAM_SOURCE_PATH = "source_path"

        /**
         * 参数 Key：携带参数
         */
        private const val PARAM_CARRY_PARAMS = "params"
    }

    override val priority: Int = 2
    override val name: String = "TrackingInterceptor"

    override fun init(context: Context) {
        Logger.d(TAG, "TrackingInterceptor 初始化完成")
    }

    /**
     * 拦截处理：上报页面跳转埋点后继续跳转流程
     *
     * @param postcard 路由信息，包含目标路径和携带参数
     * @param callback 拦截回调，始终调用 onContinue 放行
     */
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        val targetPath = postcard.path

        // 构建埋点参数
        val analyticsParams = mutableMapOf<String, String>()
        analyticsParams[PARAM_TARGET_PATH] = targetPath

        // 提取来源路径
        val sourcePath = postcard.extrasMap["source_path"]?.toString() ?: ""
        if (sourcePath.isNotEmpty()) {
            analyticsParams[PARAM_SOURCE_PATH] = sourcePath
        }

        // 提取携带的参数
        if (postcard.extrasMap.isNotEmpty()) {
            val paramsStr = postcard.extrasMap.entries.joinToString(",") { (key, value) ->
                "$key=$value"
            }
            analyticsParams[PARAM_CARRY_PARAMS] = paramsStr
        }

        // 通过 AnalyticsHelper 上报页面跳转事件
        AnalyticsHelper.logEvent(EVENT_PAGE_NAVIGATION, analyticsParams)
        Logger.d(TAG, "埋点已上报: $EVENT_PAGE_NAVIGATION → $targetPath")

        // 继续跳转流程
        callback.onContinue(postcard)
    }
}
