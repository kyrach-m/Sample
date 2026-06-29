package com.ch.sample

import android.app.Activity
import com.ch.middleware.router.RouterHelper
import com.ch.middleware.router.RouterInitializer
import com.ch.middleware.router.RouterPath
import com.ch.sample.web.WebViewActivity

/**
 * App 路由配置器
 *
 * 用于注册 app 模块中需要参与路由的页面。
 * 避免 middleware:router 层直接依赖 app 层，保持分层架构。
 *
 * 在 [BaseApplication.onCreate] 中通过 [RouterInitializer.setAppRouteConfigurator] 注册。
 */
class AppRouteConfigurator : RouterInitializer.AppRouteConfigurator {

    override fun configureRoutes() {
        // 注册主页 Activity
        RouterHelper.registerRoute(RouterPath.Main.MAIN, MainActivity::class.java as Class<out Activity>)

        // 注册 WebView Activity（用于路由降级处理）
        RouterHelper.registerRoute(RouterPath.Web.WEB_VIEW, WebViewActivity::class.java as Class<out Activity>)
    }
}
