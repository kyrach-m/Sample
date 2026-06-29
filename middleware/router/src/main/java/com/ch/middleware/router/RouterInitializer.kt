package com.ch.middleware.router

import android.content.Context
import com.ch.core.common.logger.Logger

/**
 * 路由初始化器（KSP 增强版）
 *
 * 统一管理路由模块的初始化流程：
 * 1. 初始化 [RouterHelper]（设置 Context、路径重写服务、降级服务）
 * 2. 通过反射调用各模块 KSP 生成的初始化类
 * 3. 调用 [AppRouteConfigurator] 注册 app 模块的路由
 *
 * **必须在 Application.onCreate 中调用**：
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         RouterInitializer.init(this)
 *     }
 * }
 * ```
 *
 * **KSP 自动注册**（每个模块各自生成初始化类）：
 * - [@Route] 标注的 Activity → 自动注册到路由表
 * - [@RouteInterceptorDef] 标注的拦截器 → 按优先级自动注册
 * - [@ServiceProvider] 标注的服务实现 → 自动注册到 ServiceManager
 * - [@RequireLogin] 标注的页面 → 自动标记为需要登录
 *
 * **新增模块接入方式**：
 * 1. 在模块的 `build.gradle.kts` 中应用 KSP 并添加处理器依赖
 * 2. 在 [MODULE_INIT_CLASSES] 中添加生成类名
 *
 * @see RouterHelper
 */
object RouterInitializer {

    private const val TAG = "RouterInitializer"

    /**
     * 各模块 KSP 生成的初始化类全限定名列表
     *
     * 命名规则：`com.ch.middleware.router.generated.GeneratedRouterInit_<moduleName>`
     * 其中 `<moduleName>` 是在模块 `build.gradle.kts` 的 `ksp { arg("router.moduleName", "...") }` 中配置的值。
     *
     * **新增模块时，在此添加对应的类名。**
     */
    private val MODULE_INIT_CLASSES = listOf(
        "com.ch.middleware.router.generated.GeneratedRouterInit_middleware_router",
        "com.ch.middleware.router.generated.GeneratedRouterInit_feature_login"
    )

    /**
     * App 模块路由配置器
     *
     * app 模块通过实现此接口来注册自己的路由。
     * 避免 middleware 层直接依赖 app 层，保持分层架构。
     */
    interface AppRouteConfigurator {
        fun configureRoutes()
    }

    /**
     * App 路由配置器实例
     *
     * 在 Application 初始化时由 app 模块设置。
     */
    @Volatile
    private var appConfigurator: AppRouteConfigurator? = null

    /**
     * 设置 App 路由配置器
     *
     * 应在 Application.onCreate 中，RouterInitializer.init() 之前调用。
     *
     * @param configurator App 模块提供的路由配置器
     */
    fun setAppRouteConfigurator(configurator: AppRouteConfigurator) {
        appConfigurator = configurator
    }

    /**
     * 初始化路由模块
     *
     * 执行以下操作：
     * 1. 初始化 [RouterHelper]（设置 Application Context）
     * 2. 注册路径重写服务 [GlobalPathReplaceServiceImpl]
     * 3. 注册降级服务 [GlobalDegradeServiceImpl]
     * 4. 通过反射调用各模块 KSP 生成的初始化类
     * 5. 调用 App 路由配置器注册 app 模块的路由
     *
     * @param context Application Context
     */
    fun init(context: Context) {
        Logger.d(TAG, "开始初始化路由模块...")

        // 1. 初始化 RouterHelper（设置 Context）
        RouterHelper.init(context)

        // 2. 注册路径重写服务（在拦截器之前执行）
        RouterHelper.setPathReplaceService(GlobalPathReplaceServiceImpl())

        // 3. 注册降级服务
        RouterHelper.setDegradeService(GlobalDegradeServiceImpl())

        // 4. 调用各模块 KSP 生成的初始化代码
        invokeAllModuleInits()

        // 5. 调用 App 路由配置器注册 app 模块路由
        appConfigurator?.configureRoutes()

        Logger.d(TAG, "路由模块初始化完成")
    }

    /**
     * 检查路由模块是否已初始化
     *
     * @return true 表示已初始化
     */
    fun isInitialized(): Boolean {
        return RouterHelper.isInitialized()
    }

    /**
     * 获取所有已注册的路由路径列表
     *
     * @return 路由路径列表
     */
    fun getAllRoutes(): List<String> {
        return RouterHelper.getAllRoutes()
    }

    /**
     * 获取已注册的路由数量
     *
     * @return 路由数量
     */
    fun getRouteCount(): Int {
        return RouterHelper.getRouteCount()
    }

    /**
     * 通过反射调用所有模块的 KSP 生成初始化类
     *
     * 使用反射的原因：生成代码位于各模块的 KSP 输出目录，
     * middleware:router 模块在编译期无法直接引用其他模块的生成代码。
     *
     * 如果某个模块的生成类不存在（例如未配置 KSP），会打印警告但不影响其他模块。
     */
    private fun invokeAllModuleInits() {
        var successCount = 0
        for (className in MODULE_INIT_CLASSES) {
            try {
                val clazz = Class.forName(className)
                // Kotlin object 单例通过 INSTANCE 字段访问，无需 kotlin-reflect
                val instance = try {
                    clazz.getDeclaredField("INSTANCE").get(null)
                } catch (e: NoSuchFieldException) {
                    clazz.getDeclaredConstructor().newInstance()
                }
                val initMethod = clazz.getDeclaredMethod("init")
                initMethod.invoke(instance)
                successCount++
                Logger.d(TAG, "已执行模块初始化: $className")
            } catch (e: ClassNotFoundException) {
                Logger.w(TAG, "未找到模块初始化类: $className，请确认该模块已配置 KSP")
            } catch (e: Exception) {
                Logger.e(TAG, "调用模块初始化失败: $className", e)
            }
        }
        Logger.d(TAG, "模块初始化完成：$successCount/${MODULE_INIT_CLASSES.size} 个模块已成功初始化")
    }
}
