package com.ch.middleware.router.annotation

import kotlin.reflect.KClass

/**
 * 服务提供者注解
 *
 * 标注在跨模块服务的实现类上，声明该实现对应的服务接口。
 * KSP 编译器会在编译期自动扫描并生成服务注册代码，
 * 无需手动调用 [ServiceManager.registerService]。
 *
 * 使用示例：
 * ```kotlin
 * @ServiceProvider(interfaceClass = IUserService::class)
 * class UserServiceImpl : IUserService {
 *     // ...
 * }
 * ```
 *
 * @property interfaceClass 服务接口的 KClass
 * @property singleton 是否单例（默认 true），为 true 时首次获取后缓存实例
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ServiceProvider(
    val interfaceClass: KClass<*>,
    val singleton: Boolean = true
)
