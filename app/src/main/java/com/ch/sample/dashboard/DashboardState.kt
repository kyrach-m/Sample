package com.ch.sample.dashboard

/**
 * Dashboard 页面状态
 *
 * 包含框架展示页的所有 UI 状态数据。
 *
 * @property appVersion App 版本号
 * @property buildType 构建类型（debug/release）
 * @property deviceId 设备唯一标识
 * @property screenSize 屏幕尺寸分类
 * @property systemVersion 系统版本
 * @property processCreateTime 进程创建耗时
 * @property appCreateTime Application 创建耗时
 * @property taskExecutionTime 启动任务执行耗时
 * @property totalTime 总启动耗时
 * @property memoryUsage 内存使用情况
 * @property isNetworkConnected 网络是否已连接
 * @property isStorageReady 存储是否就绪
 * @property isRouterReady 路由是否就绪
 * @property routeCount 已注册路由数量
 * @property isLoggerReady 日志系统是否就绪
 * @property isCrashHandlerReady 崩溃捕获是否就绪
 */
data class DashboardState(
    val appVersion: String = "",
    val buildType: String = "",
    val deviceId: String = "",
    val screenSize: String = "",
    val systemVersion: String = "",
    val processCreateTime: Long = 0L,
    val appCreateTime: Long = 0L,
    val taskExecutionTime: Long = 0L,
    val totalTime: Long = 0L,
    val memoryUsage: String = "",
    val isNetworkConnected: Boolean = false,
    val isStorageReady: Boolean = false,
    val isRouterReady: Boolean = false,
    val routeCount: Int = 0,
    val isLoggerReady: Boolean = false,
    val isCrashHandlerReady: Boolean = false
)
