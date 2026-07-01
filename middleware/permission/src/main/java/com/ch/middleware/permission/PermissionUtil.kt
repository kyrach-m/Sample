package com.ch.middleware.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 权限检查工具类
 *
 * 提供轻量级的权限状态检查方法，不涉及 UI 弹窗，
 * 可在任意 Context 环境下使用。
 *
 * ## 模块职责分工
 * - **本类（PermissionUtil）**：纯检查工具，判断权限是否已授予
 * - [PermissionHelper]：完整的权限请求流程（弹窗、Rationale、引导设置）
 * - [PermissionResultBus]：权限结果事件总线
 *
 * 推荐配合使用：先用本类快速检查权限状态，未授予时再调用 [PermissionHelper] 发起请求。
 *
 * 用法示例：
 * ```kotlin
 * // 检查单个权限
 * if (PermissionUtil.hasPermission(context, Manifest.permission.CAMERA)) {
 *     // 已授予，直接执行
 * } else {
 *     // 未授予，使用 PermissionHelper 发起请求
 *     val granted = permissionHelper.requestPermission(Manifest.permission.CAMERA)
 * }
 *
 * // 检查多个权限是否全部授予
 * val allGranted = PermissionUtil.hasAllPermissions(
 *     context,
 *     Manifest.permission.CAMERA,
 *     Manifest.permission.RECORD_AUDIO
 * )
 *
 * // 获取未授予的权限列表
 * val denied = PermissionUtil.getDeniedPermissions(context, *permissions)
 *
 * // 检查定位权限（含后台定位的 Android 10+ 适配）
 * val hasLocation = PermissionUtil.hasLocationPermission(context)
 *
 * // 检查通知权限（含 Android 13+ 的 POST_NOTIFICATIONS 适配）
 * val hasNotification = PermissionUtil.hasNotificationPermission(context)
 * ```
 *
 * @see PermissionHelper 完整的权限请求流程
 */
object PermissionUtil {

    // ==================== 基础检查 ====================

    /**
     * 检查单个权限是否已授予
     *
     * @param context Context
     * @param permission 权限名称（如 Manifest.permission.CAMERA）
     * @return true = 权限已授予
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否全部已授予
     *
     * @param context Context
     * @param permissions 权限名称数组
     * @return true = 全部已授予
     */
    fun hasAllPermissions(context: Context, vararg permissions: String): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    /**
     * 获取未授予的权限列表
     *
     * @param context Context
     * @param permissions 待检查的权限数组
     * @return 未授予的权限列表（空列表表示全部已授予）
     */
    fun getDeniedPermissions(context: Context, vararg permissions: String): List<String> {
        return permissions.filter { !hasPermission(context, it) }
    }

    // ==================== 常用权限快捷检查 ====================

    /**
     * 检查相机权限
     *
     * @param context Context
     * @return true = 相机权限已授予
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }

    /**
     * 检查录音权限
     *
     * @param context Context
     * @return true = 录音权限已授予
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * 检查定位权限（含 Android 10+ 后台定位适配）
     *
     * 前台定位：ACCESS_FINE_LOCATION 或 ACCESS_COARSE_LOCATION
     * 后台定位（Android 10+）：额外需要 ACCESS_BACKGROUND_LOCATION
     *
     * @param context Context
     * @param background 是否检查后台定位权限，默认 false（仅检查前台）
     * @return true = 定位权限已授予
     */
    fun hasLocationPermission(context: Context, background: Boolean = false): Boolean {
        val foregroundGranted = hasAnyPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (!foregroundGranted) return false

        // Android 10+ 后台定位需要额外权限
        return if (background && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
    }

    /**
     * 检查通知权限（含 Android 13+ 适配）
     *
     * Android 13 (API 33) 起需要动态申请 POST_NOTIFICATIONS 权限。
     * Android 12 及以下版本默认授予。
     *
     * @param context Context
     * @return true = 通知权限已授予（或系统版本不需要申请）
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Android 12 及以下版本默认允许
        }
    }

    /**
     * 检查存储权限（含 Android 13+ 细粒度媒体权限适配）
     *
     * - Android 13+：使用 READ_MEDIA_IMAGES / READ_MEDIA_VIDEO / READ_MEDIA_AUDIO
     * - Android 10-12：使用 READ_EXTERNAL_STORAGE
     * - Android 9 及以下：默认授予
     *
     * @param context Context
     * @return true = 存储读取权限已授予
     */
    fun hasStorageReadPermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+：检查细粒度媒体权限（任一即可）
                hasAnyPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12：检查 READ_EXTERNAL_STORAGE
                hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> true // Android 5 及以下默认授予
        }
    }

    /**
     * 检查精确闹钟权限（Android 12+）
     *
     * Android 12 (API 31) 起，使用 setExactAndAllowWhileIdle 等精确闹钟 API
     * 需要 SCHEDULE_EXACT_ALARM 权限。
     *
     * @param context Context
     * @return true = 精确闹钟权限已授予（或系统版本不需要）
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // SCHEDULE_EXACT_ALARM 是特殊权限，需用 AlarmManager.canScheduleExactAlarms() 检查
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.canScheduleExactAlarms()
            } catch (e: Exception) {
                false
            }
        } else {
            true
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 检查是否至少有一个权限已授予
     *
     * @param context Context
     * @param permissions 权限数组
     * @return true = 至少有一个权限已授予
     */
    private fun hasAnyPermission(context: Context, vararg permissions: String): Boolean {
        return permissions.any { hasPermission(context, it) }
    }
}
