package com.ch.sample

import com.ch.middleware.router.RouterInitializer

/**
 * App 模块路由配置器
 *
 * 实现 [RouterInitializer.AppRouteConfigurator] 接口，
 * 负责注册 app 模块的路由。
 *
 * 由于 app 模块是壳工程，主要路由都在 features 模块中，
 * 这里主要注册一些 app 专属的路由或特殊路由。
 */
class AppRouteConfigurator : RouterInitializer.AppRouteConfigurator {

    override fun configureRoutes() {
        // app 模块的路由注册（如有需要）
        // 示例：
        // RouterHelper.registerRoute("/app/main", MainActivity::class.java)
    }
}
