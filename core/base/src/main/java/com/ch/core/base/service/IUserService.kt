package com.ch.core.base.service

/**
 * 用户服务接口（跨模块服务调用）
 *
 * 定义在 [:core:base] 模块，由 [:features:feature_login] 模块提供具体实现 [com.ch.features.login.service.UserServiceImpl]。
 * 通过 [ServiceManager] 注册和获取服务实例，任意模块可调用，无需直接依赖登录模块。
 *
 * **使用方式**：
 * ```kotlin
 * // 获取服务实例
 * val userService = ServiceManager.getService(IUserService::class.java)
 *
 * // 调用方法
 * val isLoggedIn = userService.isLoggedIn()
 * val userId = userService.getCurrentUserId()
 * ```
 *
 * **设计说明**：
 * - 接口定义在公共模块，实现定义在业务模块，实现模块解耦
 * - 通过 [ServiceManager] 管理服务注册和发现
 * - 支持多模块共享用户状态，无需互相直接依赖
 *
 * @see ServiceManager
 */
interface IUserService {

    /**
     * 查询当前用户是否已登录
     *
     * @return true=已登录，false=未登录
     */
    fun isLoggedIn(): Boolean

    /**
     * 获取当前登录用户的 ID
     *
     * @return 用户 ID，未登录时返回空字符串
     */
    fun getCurrentUserId(): String

    /**
     * 获取当前登录用户的昵称
     *
     * @return 用户昵称，未登录时返回空字符串
     */
    fun getCurrentUserName(): String

    /**
     * 获取当前登录用户的头像 URL
     *
     * @return 头像 URL，未登录时返回空字符串
     */
    fun getCurrentUserAvatar(): String

    /**
     * 获取当前用户的 Token（用于接口鉴权）
     *
     * @return Token 字符串，未登录时返回空字符串
     */
    fun getToken(): String

    /**
     * 退出登录，清除用户状态和 Token（同步版本）
     *
     * 仅清除本地存储，不调用服务端接口。
     * 通常在 Token 已失效（如 401）时直接调用此方法。
     */
    fun logout()

    /**
     * 退出登录，调用服务端接口使 Token 失效（异步版本）
     *
     * 调用 POST /logout 接口通知服务端使 Token 失效，
     * 然后清除本地存储。即使服务端调用失败，也会清除本地存储。
     *
     * @return true=服务端调用成功，false=服务端调用失败（本地存储仍会清除）
     */
    suspend fun logoutAsync(): Boolean
}
