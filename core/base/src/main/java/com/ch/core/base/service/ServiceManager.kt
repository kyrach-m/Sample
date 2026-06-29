package com.ch.core.base.service

import com.ch.core.common.logger.Logger

/**
 * 服务管理器（跨模块服务发现）
 *
 * 提供统一的服务注册和发现机制，实现模块间的解耦调用。
 * 接口定义在公共模块（如 [:core:base]），实现定义在业务模块（如 [:features:feature_login]），
 * 通过 [ServiceManager] 注册和获取服务实例。
 *
 * **使用方式**：
 * ```kotlin
 * // 1. 在 Application 或业务模块中注册服务实现
 * ServiceManager.registerService(IUserService::class.java, UserServiceImpl::class.java)
 *
 * // 2. 在任意模块中获取服务实例
 * val userService = ServiceManager.getService(IUserService::class.java)
 * userService?.isLoggedIn()
 * ```
 *
 * **线程安全**：使用 ConcurrentHashMap 保证并发安全。
 *
 * @see IUserService
 */
object ServiceManager {

    private const val TAG = "ServiceManager"

    /**
     * 服务注册表：接口 Class → 实现实例
     */
    private val serviceCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Any>()

    /**
     * 服务实现类映射：接口 Class → 实现 Class
     */
    private val serviceImplMap = java.util.concurrent.ConcurrentHashMap<Class<*>, Class<*>>()

    /**
     * 注册服务实现
     *
     * 将接口类型与实现类关联。后续通过 [getService] 获取实例时，
     * 会自动创建实现类实例并缓存。
     *
     * @param T 接口类型
     * @param interfaceClass 接口 Class
     * @param implClass 实现类 Class
     */
    fun <T : Any> registerService(interfaceClass: Class<T>, implClass: Class<out T>) {
        serviceImplMap[interfaceClass] = implClass
        Logger.d(TAG, "注册服务: ${interfaceClass.simpleName} → ${implClass.simpleName}")
    }

    /**
     * 注册服务实例
     *
     * 直接注册服务实例，适用于需要传入特定实例的场景。
     *
     * @param T 接口类型
     * @param interfaceClass 接口 Class
     * @param instance 服务实现实例
     */
    fun <T : Any> registerServiceInstance(interfaceClass: Class<T>, instance: T) {
        serviceCache[interfaceClass] = instance
        Logger.d(TAG, "注册服务实例: ${interfaceClass.simpleName} → ${instance.javaClass.simpleName}")
    }

    /**
     * 获取服务实例
     *
     * 根据接口类型获取对应的服务实现实例。
     * 首次获取时会通过反射创建实现类实例并缓存。
     *
     * @param T 接口类型
     * @param interfaceClass 接口 Class
     * @return 服务实现实例，未注册返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(interfaceClass: Class<T>): T? {
        // 1. 从缓存获取
        serviceCache[interfaceClass]?.let { return it as T }

        // 2. 查找实现类
        val implClass = serviceImplMap[interfaceClass]
        if (implClass == null) {
            Logger.w(TAG, "服务未注册: ${interfaceClass.simpleName}")
            return null
        }

        // 3. 反射创建实例并缓存
        return try {
            val instance = implClass.getDeclaredConstructor().newInstance() as T
            serviceCache[interfaceClass] = instance
            Logger.d(TAG, "创建服务实例: ${interfaceClass.simpleName} → ${implClass.simpleName}")
            instance
        } catch (e: Exception) {
            Logger.e(TAG, "创建服务实例失败: ${interfaceClass.simpleName}", e)
            null
        }
    }

    /**
     * 获取服务实例（泛型便捷方法）
     *
     * 使用示例：
     * ```kotlin
     * val userService = ServiceManager.getService<IUserService>()
     * ```
     *
     * @return 服务实现实例，未注册返回 null
     */
    inline fun <reified T : Any> getService(): T? {
        return getService(T::class.java)
    }

    /**
     * 清除所有已注册服务
     *
     * 通常在测试或特殊场景下使用
     */
    fun clear() {
        serviceCache.clear()
        serviceImplMap.clear()
        Logger.d(TAG, "已清除所有服务注册")
    }
}
