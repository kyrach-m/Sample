package com.ch.middleware.router.annotation

/**
 * 需要登录才能访问的页面标记注解
 *
 * 标注在 Activity 类上，表示该页面需要用户登录后才能访问。
 * KSP 编译器会在编译期自动扫描带有此注解的页面，
 * 并将其路径自动注册到 [LoginInterceptor] 的 [requireLoginPaths] 集合中。
 *
 * 使用示例：
 * ```kotlin
 * @Route(path = "/settings/ProfileActivity")
 * @RequireLogin
 * class ProfileActivity : ComponentActivity() {
 *     // 未登录时访问此页面会被自动跳转到登录页
 * }
 * ```
 *
 * 也可在运行时手动标记：
 * ```kotlin
 * LoginInterceptor.markRequireLogin("/settings/ProfileActivity")
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class RequireLogin
