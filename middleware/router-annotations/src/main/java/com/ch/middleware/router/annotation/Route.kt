package com.ch.middleware.router.annotation

/**
 * 页面路由注解
 *
 * 标注在 Activity 类上，声明该页面的路由路径。
 * KSP 编译器会在编译期自动扫描并生成路由注册代码，无需手动调用 [RouterHelper.registerRoute]。
 *
 * 使用示例：
 * ```kotlin
 * @Route(path = "/settings/ProfileActivity", description = "个人信息页")
 * class ProfileActivity : AppCompatActivity()
 * ```
 *
 * @property path 路由路径，格式为 "/模块/页面名"，必须全局唯一
 * @property description 路由描述（可选），用于文档和调试
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(
    val path: String,
    val description: String = ""
)
