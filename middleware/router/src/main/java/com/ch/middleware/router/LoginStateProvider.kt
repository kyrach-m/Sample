package com.ch.middleware.router

/**
 * 登录状态提供者接口
 *
 * 由应用层实现，为 [LoginInterceptor] 提供当前用户的登录状态查询能力。
 * 必须在 Application 初始化时通过 [LoginInterceptor.setLoginStateProvider] 注入。
 *
 * 使用示例：
 * ```kotlin
 * // 在 Application.onCreate 中
 * LoginInterceptor.setLoginStateProvider(object : LoginStateProvider {
 *     override fun isLoggedIn(): Boolean {
 *         return UserManager.getInstance().hasToken()
 *     }
 * })
 * ```
 *
 * @see LoginInterceptor
 */
interface LoginStateProvider {

    /**
     * 查询当前用户是否已登录
     *
     * @return true=已登录，false=未登录
     */
    fun isLoggedIn(): Boolean
}
