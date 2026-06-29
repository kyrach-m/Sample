package com.ch.middleware.router.annotation

/**
 * 路由拦截器定义注解
 *
 * 标注在实现了 [RouteInterceptorV2] 的类上，声明拦截器的优先级。
 * KSP 编译器会在编译期自动扫描并按优先级生成注册代码。
 *
 * 使用示例：
 * ```kotlin
 * @RouteInterceptorDef(priority = 1)
 * class LoginInterceptor : RouteInterceptorV2 {
 *     override val priority = 1
 *     // ...
 * }
 * ```
 *
 * @property priority 拦截器优先级，数值越小越先执行
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class RouteInterceptorDef(
    val priority: Int = 0
)
