package com.ch.core.base.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * 屏幕路由常量
 *
 * 定义所有屏幕的路由路径，用于导航跳转。
 */
object Screen {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val COMPONENTS = "components"
}

/**
 * 应用导航图配置
 *
 * 通过 lambda 注入各个 Screen 的 Composable 内容，
 * 避免 core:base 模块直接依赖 app 模块的具体实现。
 *
 * 用法示例：
 * ```kotlin
 * AppNavHost(
 *     navController = navController,
 *     splashScreen = { SplashScreen() },
 *     dashboardScreen = { DashboardScreen() },
 *     componentsScreen = { ComponentsScreen() }
 * )
 * ```
 *
 * @param navController 导航控制器
 * @param startDestination 起始目的地
 * @param splashScreen 闪屏页 Composable
 * @param loginScreen 登录页 Composable
 * @param dashboardScreen 主页 Composable
 * @param componentsScreen 组件库页 Composable
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SPLASH,
    splashScreen: @Composable () -> Unit = {},
    loginScreen: @Composable () -> Unit = {},
    dashboardScreen: @Composable () -> Unit = {},
    componentsScreen: @Composable () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SPLASH) {
            splashScreen()
        }
        composable(Screen.LOGIN) {
            loginScreen()
        }
        composable(Screen.DASHBOARD) {
            dashboardScreen()
        }
        composable(Screen.COMPONENTS) {
            componentsScreen()
        }
    }
}

/**
 * 导航操作封装
 *
 * 提供常用的导航操作，避免直接使用 NavController。
 * 符合公约中"所有路由跳转必须经过封装"的要求。
 */
class AppNavigator(private val navController: NavHostController) {

    /**
     * 导航到 Dashboard 页面
     *
     * 从 Splash 页面跳转到 Dashboard 时，清空返回栈，
     * 防止用户返回到 Splash 页面。
     */
    fun navigateToDashboard() {
        navController.navigate(Screen.DASHBOARD) {
            popUpTo(Screen.SPLASH) {
                inclusive = true
            }
        }
    }

    /**
     * 导航到组件库页面
     */
    fun navigateToComponents() {
        navController.navigate(Screen.COMPONENTS)
    }

    /**
     * 导航到指定页面
     *
     * @param route 目标页面路由
     * @param popUpTo 弹出到指定页面（可选）
     * @param inclusive 是否包含弹出页面本身
     */
    fun navigateTo(
        route: String,
        popUpTo: String? = null,
        inclusive: Boolean = false
    ) {
        navController.navigate(route) {
            if (popUpTo != null) {
                popUpTo(popUpTo) {
                    this.inclusive = inclusive
                }
            }
        }
    }

    /**
     * 返回上一页
     */
    fun navigateBack() {
        navController.popBackStack()
    }
}
