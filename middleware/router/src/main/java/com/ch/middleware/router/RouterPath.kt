package com.ch.middleware.router

/**
 * 路由路径常量类
 *
 * 集中管理所有页面的路由路径，避免硬编码字符串。
 * 路径格式："/模块名/Activity名"
 *
 * 使用示例：
 * ```kotlin
 * // 跳转到主页
 * RouterHelper.navigate(RouterPath.Main.MAIN)
 *
 * // 跳转到设置页
 * RouterHelper.navigate(RouterPath.Settings.SETTINGS)
 * ```
 *
 * 路径命名规范：
 * - 一级路径为功能模块（如 /login、/main、/settings）
 * - 二级路径为具体页面（如 /LoginActivity、/MainActivity）
 * - 全部使用小写字母 + 驼峰命名
 */
object RouterPath {

    /**
     * 主模块路由
     */
    object Main {
        /** 主页面 */
        const val MAIN = "/main/MainActivity"

        /** 闪屏页面 */
        const val SPLASH = "/main/SplashActivity"
    }

    /**
     * 设置模块路由
     */
    object Settings {
        /** 设置页面 */
        const val SETTINGS = "/settings/SettingsActivity"

        /** 个人信息页面 */
        const val PROFILE = "/settings/ProfileActivity"
    }

    /**
     * Web 模块路由
     */
    object Web {
        /** 通用 WebView 页面 */
        const val WEB_VIEW = "/web/WebViewActivity"
    }
}
