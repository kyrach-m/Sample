package com.ch.middleware.router

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.ch.core.common.logger.Logger

/**
 * Deep Link 处理器
 *
 * 将外部链接（URL Scheme / App Link）映射为内部路由路径，
 * 实现从浏览器、推送通知、其他 APP 等外部入口直接跳转到应用内页面。
 *
 * ## 支持的链接格式
 *
 * | 链接格式 | 示例 | 映射路由 |
 * |---------|------|---------|
 * | `scheme://host/path` | `myapp://main/home` | `/main/MainActivity` |
 * | `https://host/path` | `https://m.example.com/settings/profile` | `/settings/ProfileActivity` |
 * | 带参数 | `myapp://web/view?url=xxx` | `/web/WebViewActivity` + Bundle |
 *
 * ## 使用方式
 *
 * ```kotlin
 * // 1. 注册 Deep Link 映射规则
 * DeepLinkHandler.addMapping("m.example.com/settings/profile", RouterPath.Settings.PROFILE)
 * DeepLinkHandler.addMapping("m.example.com/main/home", RouterPath.Main.MAIN)
 *
 * // 2. 在 Activity 中处理外部 Intent
 * intent?.data?.let { uri ->
 *     DeepLinkHandler.handle(context, uri)
 * }
 *
 * // 3. 从推送通知跳转
 * DeepLinkHandler.handle(context, Uri.parse("myapp://settings/profile?userId=123"))
 * ```
 *
 * ## 匹配优先级
 * 1. 精确匹配（host + path 完全匹配）
 * 2. 前缀匹配（path 的前缀匹配）
 * 3. 降级服务（[DegradeService]）
 *
 * @see RouterHelper
 * @see GlobalDegradeServiceImpl
 */
object DeepLinkHandler {

    private const val TAG = "DeepLinkHandler"

    /**
     * Deep Link 映射表：URI 路径 → 内部路由路径
     *
     * Key 格式：`host/path`（不含 scheme），如 `m.example.com/settings/profile`
     */
    private val mappingTable = mutableMapOf<String, String>()

    /**
     * 默认 Scheme（应用自定义 scheme，如 `myapp`）
     *
     * 通过 [setDefaultScheme] 设置，处理 Intent 时如果 URI 的 scheme 不匹配则忽略。
     */
    @Volatile
    private var defaultScheme: String? = null

    /**
     * 默认 Host（应用自定义 host）
     */
    @Volatile
    private var defaultHost: String? = null

    /**
     * 设置默认 Scheme 和 Host
     *
     * 用于过滤非本应用的 Deep Link。
     *
     * @param scheme 应用 Scheme（如 `myapp`）
     * @param host 应用 Host（如 `m.example.com`）
     */
    fun setDefaultScheme(scheme: String, host: String? = null) {
        defaultScheme = scheme
        defaultHost = host
        Logger.d(TAG, "默认 Scheme 已设置: $scheme, Host: $host")
    }

    /**
     * 添加 Deep Link 映射规则
     *
     * @param uriPath URI 路径（格式：`host/path`，如 `m.example.com/settings/profile`）
     * @param routePath 对应的内部路由路径（如 [RouterPath.Settings.PROFILE]）
     */
    fun addMapping(uriPath: String, routePath: String) {
        mappingTable[uriPath] = routePath
        Logger.d(TAG, "添加 Deep Link 映射: $uriPath → $routePath")
    }

    /**
     * 批量添加 Deep Link 映射规则
     *
     * @param mappings 映射表（URI 路径 → 路由路径）
     */
    fun addMappings(mappings: Map<String, String>) {
        mappingTable.putAll(mappings)
        Logger.d(TAG, "批量添加 ${mappings.size} 条 Deep Link 映射")
    }

    /**
     * 处理外部 Deep Link URI
     *
     * 将外部 URI 解析为内部路由并跳转。
     *
     * @param context 上下文
     * @param uri 外部 URI（如 `myapp://settings/profile?userId=123`）
     * @param skipInterceptors 是否跳过拦截器（默认 false）
     * @return true=跳转成功，false=无法处理
     */
    fun handle(context: Context, uri: Uri, skipInterceptors: Boolean = false): Boolean {
        Logger.d(TAG, "处理 Deep Link: $uri")

        // 1. Scheme 校验
        val scheme = uri.scheme?.lowercase()
        if (defaultScheme != null && scheme != null && scheme != defaultScheme && scheme != "https" && scheme != "http") {
            Logger.w(TAG, "Scheme 不匹配: $scheme (期望: $defaultScheme 或 https/http)")
            return false
        }

        // 2. Host 校验
        val host = uri.host?.lowercase()
        if (defaultHost != null && host != null && host != defaultHost) {
            Logger.w(TAG, "Host 不匹配: $host (期望: $defaultHost)")
            return false
        }

        // 3. 构建匹配 Key：host + path
        val fullPath = if (host != null) {
            "$host${uri.path ?: ""}"
        } else {
            uri.path ?: ""
        }

        // 4. 精确匹配
        val routePath = mappingTable[fullPath]
        if (routePath != null) {
            Logger.d(TAG, "精确匹配: $fullPath → $routePath")
            val params = extractQueryParams(uri)
            return RouterHelper.navigateWithParams(
                context = context,
                path = routePath,
                params = params,
                skipInterceptors = skipInterceptors
            )
        }

        // 5. 前缀匹配（从最长前缀开始）
        val matchedEntry = mappingTable.entries
            .filter { (key, _) -> fullPath.startsWith(key) }
            .maxByOrNull { it.key.length }

        if (matchedEntry != null) {
            Logger.d(TAG, "前缀匹配: $fullPath → ${matchedEntry.value} (规则: ${matchedEntry.key})")
            val params = extractQueryParams(uri)
            // 将未匹配的 path 部分作为参数传递
            val remainingPath = fullPath.removePrefix(matchedEntry.key)
            if (remainingPath.isNotEmpty()) {
                params.putString("deep_link_remaining_path", remainingPath)
            }
            return RouterHelper.navigateWithParams(
                context = context,
                path = matchedEntry.value,
                params = params,
                skipInterceptors = skipInterceptors
            )
        }

        // 6. 未匹配到，尝试降级服务
        Logger.w(TAG, "Deep Link 未匹配到路由: $fullPath")
        RouterHelper.setDegradeService(GlobalDegradeServiceImpl())
        return false
    }

    /**
     * 从 URI 中提取查询参数
     *
     * @param uri URI 对象
     * @return Bundle 形式的查询参数
     */
    private fun extractQueryParams(uri: Uri): Bundle {
        val params = Bundle()
        uri.queryParameterNames?.forEach { key ->
            val value = uri.getQueryParameter(key)
            if (value != null) {
                params.putString(key, value)
            }
        }
        // 保留原始 URI 供调试使用
        params.putString("deep_link_uri", uri.toString())
        return params
    }

    /**
     * 获取所有已注册的 Deep Link 映射
     *
     * @return 映射表（URI 路径 → 路由路径）
     */
    fun getAllMappings(): Map<String, String> = mappingTable.toMap()

    /**
     * 清除所有 Deep Link 映射
     */
    fun clearMappings() {
        mappingTable.clear()
        Logger.d(TAG, "已清除所有 Deep Link 映射")
    }
}
