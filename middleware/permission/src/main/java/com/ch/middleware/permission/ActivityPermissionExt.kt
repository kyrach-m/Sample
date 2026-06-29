package com.ch.middleware.permission

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Activity 权限扩展
 *
 * 提供便捷的权限请求能力，通过 [PermissionDelegate] 实现。
 * 使用属性委托自动管理生命周期。
 *
 * 使用示例：
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *
 *     // 方式一：使用扩展属性（推荐）
 *     private val permissions by permissionDelegate()
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // 设置权限结果回调（可选）
 *         permissions.onPermissionResult = { result ->
 *             if (result.allGranted) {
 *                 // 所有权限已授予
 *             }
 *         }
 *
 *         // 请求权限
 *         permissions.requestPermissionAsync(Manifest.permission.CAMERA) { granted ->
 *             if (granted) openCamera()
 *         }
 *     }
 * }
 * ```
 *
 * ```kotlin
 * // 方式二：在协程中使用
 * class MyActivity : AppCompatActivity() {
 *     private val permissions by permissionDelegate()
 *
 *     fun checkPermission() {
 *         lifecycleScope.launch {
 *             val granted = permissions.requestPermission(Manifest.permission.CAMERA)
 *             if (granted) {
 *                 // 权限已授予
 *             }
 *         }
 *     }
 * }
 * ```
 */

/**
 * 创建权限委托的属性委托函数
 *
 * 自动绑定 Activity 生命周期，在 onCreate 时初始化，onDestroy 时清理。
 *
 * @return [PermissionDelegate] 实例
 */
fun ComponentActivity.permissionDelegate(): Lazy<PermissionDelegate> {
    return lazy {
        PermissionDelegate(this).also { delegate ->
            // 注册生命周期观察，自动在 onCreate 时初始化
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    delegate.onCreate()
                }
            })
        }
    }
}

/**
 * 快速请求单个权限（扩展函数）
 *
 * 内部创建临时的 [PermissionDelegate]，适合一次性使用场景。
 * 如需多次请求权限，建议使用 [permissionDelegate] 创建持久委托。
 *
 * @param permission 权限名称
 * @param callback 结果回调
 */
fun ComponentActivity.requestPermissionQuick(
    permission: String,
    callback: (Boolean) -> Unit
) {
    val delegate = PermissionDelegate(this)
    delegate.onCreate()
    delegate.requestPermissionAsync(permission, callback)
}

/**
 * 快速请求多个权限（扩展函数）
 *
 * 内部创建临时的 [PermissionDelegate]，适合一次性使用场景。
 *
 * @param permissions 权限名称数组
 * @param callback 结果回调
 */
fun ComponentActivity.requestPermissionsQuick(
    vararg permissions: String,
    callback: (Map<String, Boolean>) -> Unit
) {
    val delegate = PermissionDelegate(this)
    delegate.onCreate()
    delegate.requestPermissionsAsync(*permissions, callback = callback)
}
