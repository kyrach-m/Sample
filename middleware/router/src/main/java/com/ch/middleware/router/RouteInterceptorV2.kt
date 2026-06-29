package com.ch.middleware.router

import android.os.Bundle

/**
 * 路由拦截器接口（V2 - 支持回调链式处理）
 *
 * 类似 ARouter 的 IInterceptor，支持异步拦截处理。
 * 拦截器通过 [InterceptorCallback] 决定是放行还是中断路由跳转。
 *
 * 拦截器按优先级（[priority]）从小到大依次执行：
 * - priority = 1：[LoginInterceptor]（登录拦截，最先执行）
 * - priority = 2：[TrackingInterceptor]（埋点上报，在登录拦截之后）
 *
 * 使用示例：
 * ```kotlin
 * RouterHelper.addInterceptorV2(object : RouteInterceptorV2 {
 *     override val priority = 1
 *
 *     override fun process(postcard: Postcard, callback: InterceptorCallback) {
 *         if (需要拦截) {
 *             callback.onInterrupt("拦截原因")
 *         } else {
 *             callback.onContinue(postcard)
 *         }
 *     }
 * })
 * ```
 *
 * @see LoginInterceptor
 * @see TrackingInterceptor
 */
interface RouteInterceptorV2 {

    /**
     * 拦截器优先级
     *
     * 数值越小优先级越高，越先执行。
     */
    val priority: Int

    /**
     * 拦截器名称（用于日志标识）
     */
    val name: String
        get() = this.javaClass.simpleName

    /**
     * 拦截处理
     *
     * @param postcard 路由信息，包含目标路径、参数等
     * @param callback 拦截回调
     */
    fun process(postcard: Postcard, callback: InterceptorCallback)

    /**
     * 初始化回调
     *
     * 在拦截器首次注册时调用，可用于执行初始化逻辑。
     *
     * @param context Android Context
     */
    fun init(context: android.content.Context) {}
}

/**
 * 路由信息载体
 *
 * 封装一次路由跳转的所有信息，包括目标路径、携带参数等。
 * 在拦截器链中传递，每个拦截器可以读取或修改其中的数据。
 *
 * @property path 目标路由路径
 * @property extras 携带的参数（Bundle）
 * @property extrasMap 携带的参数（Map 形式，便于读取）
 */
data class Postcard(
    val path: String,
    var extras: Bundle? = null,
    val extrasMap: MutableMap<String, Any?> = mutableMapOf()
) {
    /**
     * 添加字符串参数
     */
    fun withString(key: String, value: String): Postcard {
        extrasMap[key] = value
        ensureBundle()
        extras?.putString(key, value)
        return this
    }

    /**
     * 添加整数参数
     */
    fun withInt(key: String, value: Int): Postcard {
        extrasMap[key] = value
        ensureBundle()
        extras?.putInt(key, value)
        return this
    }

    /**
     * 添加长整型参数
     */
    fun withLong(key: String, value: Long): Postcard {
        extrasMap[key] = value
        ensureBundle()
        extras?.putLong(key, value)
        return this
    }

    /**
     * 添加布尔参数
     */
    fun withBoolean(key: String, value: Boolean): Postcard {
        extrasMap[key] = value
        ensureBundle()
        extras?.putBoolean(key, value)
        return this
    }

    /**
     * 确保 Bundle 已初始化
     */
    private fun ensureBundle() {
        if (extras == null) {
            extras = Bundle()
        }
    }
}

/**
 * 拦截器回调接口
 *
 * 用于在拦截器中控制路由跳转流程：
 * - [onContinue]：放行，继续执行下一个拦截器或最终跳转
 * - [onInterrupt]：中断，终止路由跳转
 */
interface InterceptorCallback {

    /**
     * 继续跳转流程
     *
     * 调用后，路由将传递给下一个拦截器处理。
     * 如果已经是最后一个拦截器，则执行实际跳转。
     *
     * @param postcard 路由信息
     */
    fun onContinue(postcard: Postcard)

    /**
     * 中断跳转
     *
     * 调用后，路由跳转被终止，不会继续传递给后续拦截器。
     *
     * @param reason 中断原因（可为 null），用于日志记录
     */
    fun onInterrupt(reason: String?)
}
