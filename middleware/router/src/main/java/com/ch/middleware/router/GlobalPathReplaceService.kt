package com.ch.middleware.router

import android.content.Context
import com.ch.core.common.logger.Logger

/**
 * 路径重写服务接口
 *
 * 在路由跳转前，对目标路径进行动态重写/替换。
 *
 * @see GlobalPathReplaceServiceImpl
 */
interface PathReplaceService {
    /**
     * 路径重写
     *
     * @param path 原始路由路径
     * @return 重写后的路径（若无需重写则返回原路径）
     */
    fun forString(path: String): String
}

/**
 * 全局路径重写服务实现
 *
 * 在路由跳转前，对目标路径进行动态重写/替换。
 * 适用于以下场景：
 *
 * - **路径迁移**：旧版路由重定向到新版路由（如 `/old/page` → `/new/page`）
 * - **AB 测试**：根据实验分组动态修改目标页面
 * - **环境切换**：Debug/Release 环境使用不同页面
 * - **灰度发布**：根据用户标签跳转到不同版本的页面
 *
 * **执行时机**：在拦截器之前执行，所有后续拦截器和路由查找都基于重写后的路径。
 *
 * **使用示例**：
 * ```kotlin
 * // 路径重写映射表（可从服务端动态下发）
 * // /legacy/settings → /settings/SettingsActivity
 * // /legacy/profile  → /settings/ProfileActivity
 * ```
 *
 * @see GlobalDegradeServiceImpl
 */
class GlobalPathReplaceServiceImpl : PathReplaceService {

    companion object {
        private const val TAG = "PathReplaceService"

        /**
         * 路径重写映射表（旧路径 → 新路径）
         *
         * 实际项目中可从服务端动态下发，或根据 AB 测试配置动态修改。
         */
        private val PATH_REWRITE_MAP = mutableMapOf(
            "/legacy/settings" to RouterPath.Settings.SETTINGS,
            "/legacy/profile" to RouterPath.Settings.PROFILE,
            "/legacy/main" to RouterPath.Main.MAIN
        )

        /**
         * 动态添加路径重写规则
         *
         * @param oldPath 旧路径
         * @param newPath 新路径
         */
        fun addRewriteRule(oldPath: String, newPath: String) {
            PATH_REWRITE_MAP[oldPath] = newPath
            Logger.d(TAG, "添加重写规则: $oldPath → $newPath")
        }

        /**
         * 批量添加路径重写规则
         *
         * @param rules 重写规则映射表
         */
        fun addRewriteRules(rules: Map<String, String>) {
            PATH_REWRITE_MAP.putAll(rules)
            Logger.d(TAG, "批量添加 ${rules.size} 条重写规则")
        }
    }

    /**
     * 路径重写
     *
     * 在路由查找前调用，返回重写后的路径。
     * 如果路径不需要重写，直接返回原路径。
     *
     * @param path 原始路由路径
     * @return 重写后的路径（若无需重写则返回原路径）
     */
    override fun forString(path: String): String {
        val newPath = PATH_REWRITE_MAP[path]
        if (newPath != null) {
            Logger.d(TAG, "路径重写: $path → $newPath")
            return newPath
        }
        return path
    }
}
