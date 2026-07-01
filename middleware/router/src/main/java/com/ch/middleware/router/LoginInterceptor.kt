package com.ch.middleware.router

import android.content.Context
import com.ch.core.common.logger.Logger
import com.ch.middleware.router.annotation.RouteInterceptorDef

/**
 * 全局登录拦截器
 *
 * 基于 [RouteInterceptorV2] 实现，在路由跳转前检查目标页面是否需要登录。
 * 目标页面通过 [@RequireLogin] 注解标记，拦截器通过 [requireLoginPaths] 集合判断。
 *
 * **执行优先级**：priority = 1，确保在 [TrackingInterceptor]（priority = 2）之前执行，
 * 未登录时不会触发埋点上报。
 *
 * **拦截流程**：
 * 1. 获取目标页面路径 [Postcard.path]
 * 2. 检查目标路径是否在 [requireLoginPaths] 中
 * 3. 若需要登录且用户未登录 → 调用 [InterceptorCallback.onInterrupt] 中断跳转
 * 4. 通过 [RouterHelper] 跳转到登录页（使用 `skipInterceptors = true` 避免无限递归）
 * 5. 若已登录或不需要登录 → 调用 [InterceptorCallback.onContinue] 放行
 *
 * **初始化**：必须在 Application.onCreate 中设置 [LoginStateProvider]：
 * ```kotlin
 * LoginInterceptor.setLoginStateProvider(object : LoginStateProvider {
 *     override fun isLoggedIn() = UserManager.hasToken()
 * })
 * ```
 *
 * @see com.ch.middleware.router.annotation.RequireLogin
 * @see LoginStateProvider
 * @see TrackingInterceptor
 */
@RouteInterceptorDef(priority = 1)
class LoginInterceptor : RouteInterceptorV2 {

    companion object {
        private const val TAG = "LoginInterceptor"

        /**
         * 登录状态提供者（由应用层注入）
         */
        @Volatile
        private var loginStateProvider: LoginStateProvider? = null

        /**
         * 需要登录才能访问的路径集合
         *
         * 通过 [markRequireLogin] 添加，拦截器会检查目标路径是否在此集合中。
         */
        private val requireLoginPaths = mutableSetOf<String>()

        /**
         * 登录页路由路径
         *
         * 框架层不硬编码业务路径，必须由应用层通过 [setLoginPath] 配置。
         * 若未配置，拦截后仅记录日志不跳转，避免框架依赖业务模块。
         */
        @Volatile
        private var loginPath: String? = null

        /**
         * 设置登录状态提供者
         *
         * 必须在 Application.onCreate 中调用，否则拦截器无法判断登录状态。
         *
         * @param provider 登录状态查询实现
         */
        fun setLoginStateProvider(provider: LoginStateProvider) {
            loginStateProvider = provider
            Logger.d(TAG, "LoginStateProvider 已设置: ${provider.javaClass.simpleName}")
        }

        /**
         * 设置登录页路由路径
         *
         * 框架层不硬编码业务路径，必须由应用层（如 features:login）配置。
         * 应在 Application.onCreate 中调用。
         *
         * @param path 登录页路由路径（如 "/login/LoginActivity"）
         */
        fun setLoginPath(path: String) {
            loginPath = path
            Logger.d(TAG, "登录页路径已设置: $path")
        }

        /**
         * 获取当前登录页路由路径
         *
         * @return 登录页路径，未配置时返回 null
         */
        fun getLoginPath(): String? = loginPath

        /**
         * 获取当前登录状态提供者
         */
        fun getLoginStateProvider(): LoginStateProvider? = loginStateProvider

        /**
         * 标记某个路径需要登录才能访问
         *
         * @param path 需要登录的路径
         */
        fun markRequireLogin(path: String) {
            requireLoginPaths.add(path)
        }

        /**
         * 批量标记路径需要登录
         *
         * @param paths 需要登录的路径集合
         */
        fun markRequireLogin(vararg paths: String) {
            requireLoginPaths.addAll(paths)
        }

        /**
         * 检查路径是否需要登录
         *
         * @param path 路由路径
         * @return true=需要登录
         */
        fun isRequireLogin(path: String): Boolean = requireLoginPaths.contains(path)
    }

    override val priority: Int = 1
    override val name: String = "LoginInterceptor"

    override fun init(context: Context) {
        Logger.d(TAG, "LoginInterceptor 初始化完成")
    }

    /**
     * 拦截处理
     *
     * @param postcard 路由信息，包含目标路径、参数等
     * @param callback 拦截回调，调用 onContinue 放行或 onInterrupt 中断
     */
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        val targetPath = postcard.path

        // 1. 获取登录状态提供者
        val provider = loginStateProvider
        if (provider == null) {
            Logger.w(TAG, "LoginStateProvider 未设置，默认放行: $targetPath")
            callback.onContinue(postcard)
            return
        }

        // 2. 检查目标路径是否需要登录
        if (!requireLoginPaths.contains(targetPath)) {
            // 不需要登录，直接放行
            callback.onContinue(postcard)
            return
        }

        // 3. 需要登录 → 检查登录状态
        if (provider.isLoggedIn()) {
            // 已登录，放行
            Logger.d(TAG, "用户已登录，放行: $targetPath")
            callback.onContinue(postcard)
        } else {
            // 未登录 → 中断跳转，重定向到登录页
            callback.onInterrupt("用户未登录，需要跳转到登录页")

            // 4. 跳转到登录页（skipInterceptors = true 避免无限递归）
            val path = loginPath
            if (path == null) {
                // 框架层不硬编码业务路径，应用层必须通过 setLoginPath() 配置
                Logger.w(TAG, "loginPath 未配置，无法跳转登录页。请在 Application 中调用 LoginInterceptor.setLoginPath()")
                return
            }
            RouterHelper.navigateWithParams(
                context = RouterHelper.appContext ?: return,
                path = path,
                params = android.os.Bundle().apply {
                    putString("redirect_path", targetPath)
                },
                skipInterceptors = true
            )
        }
    }
}
