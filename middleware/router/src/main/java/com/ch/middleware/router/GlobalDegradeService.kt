package com.ch.middleware.router

import android.content.Context
import android.os.Bundle
import com.ch.core.common.logger.Logger

/**
 * 路由降级服务接口
 *
 * 当路由找不到目标页面（路由未注册或页面不存在）时，
 * 由该服务统一处理降级逻辑，避免应用崩溃或显示空白页面。
 *
 * @see GlobalDegradeServiceImpl
 */
interface DegradeService {
    /**
     * 路由丢失回调
     *
     * @param context 上下文
     * @param path 目标路由路径
     */
    fun onLost(context: Context, path: String)
}

/**
 * 全局路由降级服务实现
 *
 * 当 [RouterHelper] 找不到目标页面时，由该服务统一处理降级逻辑。
 *
 * **降级策略**：
 * 1. 跳转到统一的「页面不存在」H5 备用页面（通过 WebView 加载）
 * 2. 记录错误日志，便于排查路由配置问题
 *
 * **使用场景**：
 * - 动态下发路由未注册的新页面
 * - 旧版本客户端访问新版本才有的页面
 * - 路由路径拼写错误时的友好提示
 *
 * @see GlobalPathReplaceServiceImpl
 */
class GlobalDegradeServiceImpl : DegradeService {

    companion object {
        private const val TAG = "GlobalDegradeService"

        /**
         * H5 备用页面 URL（页面不存在时展示）
         *
         * 可替换为实际的业务 H5 地址
         */
        private const val FALLBACK_H5_URL = "https://m.example.com/404"

        /**
         * 页面不存在提示页路由路径
         */
        private const val PATH_PAGE_NOT_FOUND = "/web/WebViewActivity"
    }

    /**
     * 路由丢失处理
     *
     * 当目标页面路由未注册或找不到对应 Activity 时触发。
     * 默认策略：跳转到 WebView 页面加载 H5 备用页面。
     *
     * @param context 上下文
     * @param path 目标路由路径
     */
    override fun onLost(context: Context, path: String) {
        Logger.e(TAG, "路由丢失: $path → 执行降级处理")

        if (RouterHelper.isRouteAvailable(PATH_PAGE_NOT_FOUND)) {
            // 跳转到 WebView 页面加载 H5 备用页面
            val params = Bundle().apply {
                putString("url", FALLBACK_H5_URL)
                putString("title", "页面加载中")
                putString("original_path", path) // 记录原始路径用于日志
            }
            RouterHelper.navigateWithParams(
                context = context,
                path = PATH_PAGE_NOT_FOUND,
                params = params,
                skipInterceptors = true
            )
        } else {
            Logger.e(TAG, "WebView 备用页面也未注册，无法降级: $PATH_PAGE_NOT_FOUND")
        }
    }
}
