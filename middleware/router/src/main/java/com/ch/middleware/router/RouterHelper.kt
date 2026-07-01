package com.ch.middleware.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ch.core.common.logger.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * 路由助手（商业级增强版）
 *
 * 轻量级页面路由封装，基于 Intent 实现。
 * 提供统一的页面跳转 API，支持拦截器链、路径重写、降级服务等高级功能。
 *
 * 核心功能：
 * - [navigate]：基础页面跳转
 * - [navigateWithParams]：携带 Bundle 参数的页面跳转
 * - [navigateWithMap]：携带键值对参数的页面跳转
 * - [navigateForResult]：带请求码的页面跳转
 * - [registerRoute]：注册路由映射（路径 → Activity 类）
 * - [addInterceptorV2]：添加 V2 拦截器（支持回调链式处理）
 *
 * 高级功能：
 * - **拦截器链**：按优先级依次执行 [RouteInterceptorV2]，支持异步回调
 * - **路径重写**：通过 [PathReplaceService] 在跳转前动态修改目标路径
 * - **降级服务**：通过 [DegradeService] 处理路由未找到的场景
 * - **跳过拦截器**：`skipInterceptors = true` 跳过所有 V2 拦截器（用于登录页跳转等）
 *
 * 使用示例：
 * ```kotlin
 * // 1. 在 Application 中注册路由
 * RouterHelper.registerRoute(RouterPath.Login.LOGIN, LoginActivity::class.java)
 * RouterHelper.registerRoute(RouterPath.Main.MAIN, MainActivity::class.java)
 *
 * // 2. 初始化拦截器和服务
 * RouterHelper.setPathReplaceService(GlobalPathReplaceServiceImpl())
 * RouterHelper.setDegradeService(GlobalDegradeServiceImpl())
 * RouterHelper.addInterceptorV2(LoginInterceptor())
 * RouterHelper.addInterceptorV2(TrackingInterceptor())
 *
 * // 3. 基础跳转（自动经过路径重写 → 拦截器链 → 实际跳转）
 * RouterHelper.navigate(context, RouterPath.Login.LOGIN)
 *
 * // 4. 跳过拦截器跳转
 * RouterHelper.navigate(context, RouterPath.Login.LOGIN, skipInterceptors = true)
 * ```
 *
 * 设计说明：
 * - 使用路由表（Map）管理路径到 Activity 的映射
 * - 支持 V1 拦截器（[RouteInterceptor]，同步）和 V2 拦截器（[RouteInterceptorV2]，异步回调链）
 * - 路径重写在拦截器之前执行
 * - 降级服务在路由未找到时触发
 * - 线程安全的路由表管理
 */
object RouterHelper {

    private const val TAG = "RouterHelper"

    /**
     * 路由表：路径 → Activity 类
     *
     * 使用 [ConcurrentHashMap] 保证多线程注册/查询的线程安全。
     */
    private val routeTable = ConcurrentHashMap<String, Class<out Activity>>()

    /**
     * V1 路由拦截器列表（同步，简单拦截）
     *
     * 使用 [CopyOnWriteArrayList] 保证读操作无锁、写操作线程安全。
     */
    private val interceptors = java.util.concurrent.CopyOnWriteArrayList<RouteInterceptor>()

    /**
     * V2 路由拦截器列表（异步回调链，按优先级排序）
     *
     * 使用 [CopyOnWriteArrayList] 保证读操作无锁、写操作线程安全。
     */
    private val interceptorsV2 = java.util.concurrent.CopyOnWriteArrayList<RouteInterceptorV2>()

    /**
     * 路径重写服务
     */
    @Volatile
    private var pathReplaceService: PathReplaceService? = null

    /**
     * 降级服务
     */
    @Volatile
    private var degradeService: DegradeService? = null

    /**
     * Application Context（用于拦截器内部跳转）
     */
    @Volatile
    var appContext: Context? = null
        private set

    /**
     * 已注册的路由数量
     */
    private var routeCount = 0

    /**
     * 是否已初始化
     */
    @Volatile
    private var isInitialized = false

    /**
     * 初始化 RouterHelper
     *
     * 必须在 Application.onCreate 中调用。
     *
     * @param context Application Context
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        isInitialized = true
        Logger.d(TAG, "RouterHelper 初始化完成")
    }

    /**
     * 检查是否已初始化
     *
     * @return true 表示已初始化
     */
    fun isInitialized(): Boolean {
        return isInitialized && appContext != null
    }

    /**
     * 获取所有已注册的路由路径列表
     *
     * @return 路由路径列表
     */
    fun getAllRoutes(): List<String> {
        return routeTable.keys.sorted()
    }

    /**
     * 注册路由映射
     *
     * 将路径与 Activity 类关联。必须在跳转前注册。
     *
     * @param path 路由路径（参考 [RouterPath]）
     * @param activityClass 目标 Activity 类
     */
    fun registerRoute(path: String, activityClass: Class<out Activity>) {
        routeTable[path] = activityClass
        routeCount++
        Logger.d(TAG, "注册路由: $path → ${activityClass.simpleName}")
    }

    /**
     * 批量注册路由
     *
     * @param routes 路由映射（路径 → Activity 类）
     */
    fun registerRoutes(routes: Map<String, Class<out Activity>>) {
        routeTable.putAll(routes)
        Logger.d(TAG, "批量注册 ${routes.size} 条路由")
    }

    /**
     * 添加 V1 路由拦截器（同步）
     *
     * @param interceptor 拦截器
     */
    fun addInterceptor(interceptor: RouteInterceptor) {
        interceptors.add(interceptor)
    }

    /**
     * 添加 V2 路由拦截器（异步回调链）
     *
     * 拦截器按 [RouteInterceptorV2.priority] 从小到大排序。
     *
     * @param interceptor V2 拦截器
     */
    fun addInterceptorV2(interceptor: RouteInterceptorV2) {
        interceptorsV2.add(interceptor)
        // 按优先级排序
        interceptorsV2.sortBy { it.priority }
        // 初始化拦截器
        appContext?.let { interceptor.init(it) }
        Logger.d(TAG, "添加 V2 拦截器: ${interceptor.name} (priority=${interceptor.priority})")
    }

    /**
     * 设置路径重写服务
     *
     * @param service 路径重写服务实现
     */
    fun setPathReplaceService(service: PathReplaceService) {
        pathReplaceService = service
        Logger.d(TAG, "路径重写服务已设置: ${service.javaClass.simpleName}")
    }

    /**
     * 设置降级服务
     *
     * @param service 降级服务实现
     */
    fun setDegradeService(service: DegradeService) {
        degradeService = service
        Logger.d(TAG, "降级服务已设置: ${service.javaClass.simpleName}")
    }

    /**
     * 基础页面跳转
     *
     * @param context 上下文
     * @param path 路由路径（参考 [RouterPath]）
     * @param flags Intent flags（可选，如 FLAG_ACTIVITY_NEW_TASK）
     * @param skipInterceptors 是否跳过 V2 拦截器（默认 false）
     * @return true=跳转成功，false=路由不存在或被拦截
     */
    fun navigate(context: Context, path: String, flags: Int = 0, skipInterceptors: Boolean = false): Boolean {
        return navigateInternal(context, path, null, flags, -1, skipInterceptors)
    }

    /**
     * 携带 Bundle 参数的页面跳转
     *
     * @param context 上下文
     * @param path 路由路径
     * @param params Bundle 参数
     * @param flags Intent flags（可选）
     * @param skipInterceptors 是否跳过 V2 拦截器
     * @return true=跳转成功，false=路由不存在或被拦截
     */
    fun navigateWithParams(context: Context, path: String, params: Bundle, flags: Int = 0, skipInterceptors: Boolean = false): Boolean {
        return navigateInternal(context, path, params, flags, -1, skipInterceptors)
    }

    /**
     * 携带键值对参数的页面跳转
     *
     * @param context 上下文
     * @param path 路由路径
     * @param params 键值对参数
     * @param flags Intent flags（可选）
     * @param skipInterceptors 是否跳过 V2 拦截器
     * @return true=跳转成功，false=路由不存在或被拦截
     */
    fun navigateWithMap(context: Context, path: String, params: Map<String, Any?>, flags: Int = 0, skipInterceptors: Boolean = false): Boolean {
        val bundle = Bundle()
        for ((key, value) in params) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Bundle -> bundle.putBundle(key, value)
                null -> bundle.putString(key, null)
                else -> bundle.putString(key, value.toString())
            }
        }
        return navigateInternal(context, path, bundle, flags, -1, skipInterceptors)
    }

    /**
     * 带请求码的页面跳转
     *
     * @param activity 发起跳转的 Activity
     * @param path 路由路径
     * @param requestCode 请求码
     * @param params Bundle 参数（可选）
     * @param skipInterceptors 是否跳过 V2 拦截器
     * @return true=跳转成功，false=路由不存在或被拦截
     */
    fun navigateForResult(
        activity: Activity,
        path: String,
        requestCode: Int,
        params: Bundle? = null,
        skipInterceptors: Boolean = false
    ): Boolean {
        return navigateInternal(activity, path, params, 0, requestCode, skipInterceptors)
    }

    /**
     * 检查路由路径是否已注册
     *
     * @param path 路由路径
     * @return true=路由存在，false=路由不存在
     */
    fun isRouteAvailable(path: String): Boolean {
        return routeTable.containsKey(path)
    }

    /**
     * 获取所有已注册的路由路径
     *
     * @return 路由路径集合
     * @deprecated 请使用 [getAllRoutes]，返回已排序的路径列表
     */
    @Deprecated("使用 getAllRoutes() 代替", ReplaceWith("getAllRoutes()"))
    fun getRegisteredRoutes(): Set<String> = routeTable.keys.toSet()

    /**
     * 获取已注册的路由数量
     *
     * @return 路由数量
     */
    fun getRouteCount(): Int = routeCount

    /**
     * 打印所有已注册的路由（用于调试）
     *
     * @return 格式化的路由列表字符串
     */
    fun printAllRoutes(): String {
        val sb = StringBuilder()
        sb.appendLine("========== 路由表 (共 ${routeCount} 条) ==========")
        routeTable.forEach { (path, clazz) ->
            sb.appendLine("$path → ${clazz.simpleName}")
        }
        sb.appendLine("========================================")
        return sb.toString()
    }

    // ==================== Debug 调试面板 ====================

    /**
     * 获取路由模块完整调试信息
     *
     * 包含路由表、拦截器链、服务配置等全部信息，
     * 用于 Debug 模式下的路由调试面板展示。
     *
     * @return 格式化的调试信息字符串
     */
    fun getDebugInfo(): String {
        val sb = StringBuilder()
        sb.appendLine("╔══════════════════════════════════════╗")
        sb.appendLine("║         Router Debug Panel           ║")
        sb.appendLine("╠══════════════════════════════════════╣")

        // 基本信息
        sb.appendLine("║ 初始化状态: ${if (isInitialized) "✅ 已初始化" else "❌ 未初始化"}")
        sb.appendLine("║ 路由数量: $routeCount")
        sb.appendLine("║ V1 拦截器: ${interceptors.size} 个")
        sb.appendLine("║ V2 拦截器: ${interceptorsV2.size} 个")
        sb.appendLine("║ 路径重写: ${if (pathReplaceService != null) "✅ 已设置" else "❌ 未设置"}")
        sb.appendLine("║ 降级服务: ${if (degradeService != null) "✅ 已设置" else "❌ 未设置"}")
        sb.appendLine("╠══════════════════════════════════════╣")

        // 路由表
        sb.appendLine("║ 📍 路由表:")
        routeTable.entries.sortedBy { it.key }.forEach { (path, clazz) ->
            sb.appendLine("║   $path → ${clazz.simpleName}")
        }
        sb.appendLine("╠══════════════════════════════════════╣")

        // 拦截器链
        sb.appendLine("║ 🔗 拦截器链 (按优先级排序):")
        interceptorsV2.forEachIndexed { index, interceptor ->
            sb.appendLine("║   [${index + 1}] ${interceptor.name} (priority=${interceptor.priority})")
        }
        sb.appendLine("╠══════════════════════════════════════╣")

        // Deep Link 映射
        val deepLinkMappings = DeepLinkHandler.getAllMappings()
        sb.appendLine("║ 🔗 Deep Link 映射: ${deepLinkMappings.size} 条")
        deepLinkMappings.forEach { (uriPath, routePath) ->
            sb.appendLine("║   $uriPath → $routePath")
        }

        sb.appendLine("╚══════════════════════════════════════╝")
        return sb.toString()
    }

    /**
     * 获取拦截器链信息
     *
     * @return 拦截器列表，每项包含名称和优先级
     */
    fun getInterceptorChainInfo(): List<Pair<String, Int>> {
        return interceptorsV2.map { it.name to it.priority }
    }

    /**
     * 路由健康检查
     *
     * 检查所有已注册路由的有效性：
     * - Activity 类是否可访问
     * - 路由路径格式是否规范
     *
     * @return 检查结果列表，空列表表示全部健康
     */
    fun checkRouteHealth(): List<String> {
        val issues = mutableListOf<String>()

        routeTable.forEach { (path, clazz) ->
            // 检查路径格式
            if (!path.startsWith("/")) {
                issues.add("路径格式不规范（应以 / 开头）: $path → ${clazz.simpleName}")
            }

            // 检查 Activity 类是否可访问
            try {
                clazz.getDeclaredConstructor()
            } catch (e: Exception) {
                issues.add("Activity 类无法实例化: $path → ${clazz.name}")
            }
        }

        // 检查重复路径（理论上 ConcurrentHashMap 不会重复，但保留检查）
        val pathGroups = routeTable.keys.groupBy { it.lowercase() }
        pathGroups.filter { it.value.size > 1 }.forEach { (_, paths) ->
            issues.add("存在大小写不同的重复路径: $paths")
        }

        return issues
    }

    /**
     * 内部跳转实现（含路径重写 + V2 拦截器链 + 降级服务）
     */
    private fun navigateInternal(
        context: Context,
        rawPath: String,
        params: Bundle?,
        flags: Int,
        requestCode: Int,
        skipInterceptors: Boolean
    ): Boolean {
        // 1. 路径重写（在拦截器之前执行）
        val path = pathReplaceService?.forString(rawPath) ?: rawPath

        // 2. 查找目标 Activity
        val targetClass = routeTable[path]
        if (targetClass == null) {
            Logger.e(TAG, "路由未注册: $path")
            // 降级服务处理
            degradeService?.onLost(context, path)
            return false
        }

        // 3. V1 拦截器检查（同步）
        for (interceptor in interceptors) {
            if (!interceptor.intercept(path, params)) {
                Logger.d(TAG, "路由被 V1 拦截: $path (拦截器: ${interceptor.javaClass.simpleName})")
                return false
            }
        }

        // 4. V2 拦截器链检查（异步回调）
        if (!skipInterceptors && interceptorsV2.isNotEmpty()) {
            val postcard = Postcard(
                path = path,
                extras = params?.let { Bundle(it) }, // 拷贝一份
                extrasMap = mutableMapOf<String, Any?>().apply {
                    if (params != null) {
                        for (key in params.keySet()) {
                            put(key, params.get(key))
                        }
                    }
                }
            )

            executeInterceptorChain(postcard, context, targetClass, flags, requestCode)
            return true // 异步执行，此处返回 true
        }

        // 5. 直接跳转（无 V2 拦截器或跳过拦截器）
        return performNavigation(context, targetClass, path, params, flags, requestCode)
    }


    /**
     * 执行 V2 拦截器链
     *
     * 按优先级依次执行拦截器，每个拦截器通过 callback 决定是否放行。
     *
     * @param postcard 路由信息
     * @param context 上下文
     * @param targetClass 目标 Activity 类
     * @param flags Intent flags
     * @param requestCode 请求码
     */
    private fun executeInterceptorChain(
        postcard: Postcard,
        context: Context,
        targetClass: Class<out Activity>,
        flags: Int,
        requestCode: Int
    ) {
        var currentIndex = 0

        fun executeNext() {
            if (currentIndex >= interceptorsV2.size) {
                // 所有拦截器都已通过，执行实际跳转
                performNavigation(
                    context = context,
                    targetClass = targetClass,
                    path = postcard.path,
                    params = postcard.extras,
                    flags = flags,
                    requestCode = requestCode,
                    enterAnim = postcard.enterAnim,
                    exitAnim = postcard.exitAnim
                )
                return
            }

            val interceptor = interceptorsV2[currentIndex]
            currentIndex++

            interceptor.process(postcard, object : InterceptorCallback {
                override fun onContinue(postcard: Postcard) {
                    // 放行，执行下一个拦截器
                    executeNext()
                }

                override fun onInterrupt(reason: String?) {
                    // 中断跳转
                    Logger.d(TAG, "路由被 V2 拦截: ${postcard.path} (拦截器: ${interceptor.name}, 原因: $reason)")
                }
            })
        }

        executeNext()
    }

    /**
     * 执行实际的页面跳转
     *
     * @param enterAnim 进入动画资源 ID（0 表示无动画）
     * @param exitAnim 退出动画资源 ID（0 表示无动画）
     */
    private fun performNavigation(
        context: Context,
        targetClass: Class<out Activity>,
        path: String,
        params: Bundle?,
        flags: Int,
        requestCode: Int,
        enterAnim: Int = 0,
        exitAnim: Int = 0
    ): Boolean {
        return try {
            val intent = Intent(context, targetClass)
            if (params != null) {
                intent.putExtras(params)
            }
            if (flags != 0) {
                intent.addFlags(flags)
            }

            if (requestCode >= 0 && context is Activity) {
                @Suppress("DEPRECATION")
                context.startActivityForResult(intent, requestCode)
            } else {
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            // 应用过渡动画
            if (context is Activity && (enterAnim != 0 || exitAnim != 0)) {
                @Suppress("DEPRECATION")
                context.overridePendingTransition(enterAnim, exitAnim)
            }

            Logger.d(TAG, "路由跳转成功: $path → ${targetClass.simpleName}")
            true
        } catch (e: Exception) {
            Logger.e(TAG, "路由跳转失败: $path", e)
            false
        }
    }
}

/**
 * 路由拦截器接口（V1 - 同步简单拦截）
 *
 * 用于在路由跳转前进行同步拦截判断。
 * 典型场景：简单的条件检查等。
 * 对于需要异步处理的场景（如登录拦截），请使用 [RouteInterceptorV2]。
 */
fun interface RouteInterceptor {
    /**
     * 拦截判断
     *
     * @param path 目标路由路径
     * @param params 携带的参数
     * @return true=放行继续跳转，false=拦截终止跳转
     */
    fun intercept(path: String, params: Bundle?): Boolean
}
