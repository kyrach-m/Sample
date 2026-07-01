package com.ch.middleware.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ch.core.common.logger.Logger
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 权限请求助手（商业级增强版）
 *
 * 将 Android 权限请求封装为挂起函数，简化权限请求流程。
 * 在基础版基础上增加了 Rationale 弹窗解释、永久拒绝引导和结果总线分发。
 *
 * 核心特性：
 * - 挂起函数封装，无需手动处理回调
 * - 支持同时请求多个权限
 * - [requestPermissionWithRationale]：申请前先展示解释弹窗，用户确认后再发起系统申请
 * - 被拒后引导逻辑：检测"不再询问"状态，弹窗引导用户前往系统设置页手动开启
 * - 结果总线集成：申请完成后自动通过 [PermissionResultBus] 广播结果
 * - 自动检查已授予的权限（避免重复弹窗）
 * - 线程安全
 *
 * 使用示例：
 * ```kotlin
 * val permissionHelper = PermissionHelper(this)
 *
 * // 基础权限申请
 * val granted = permissionHelper.requestPermission(Manifest.permission.CAMERA)
 *
 * // 带 Rationale 解释弹窗的权限申请
 * val granted = permissionHelper.requestPermissionWithRationale(
 *     permission = Manifest.permission.CAMERA,
 *     rationaleTitle = "相机权限说明",
 *     rationaleMessage = "我们需要相机权限来拍摄照片，请允许访问。"
 * )
 *
 * // 带 Rationale 的多权限申请
 * val results = permissionHelper.requestPermissionsWithRationale(
 *     permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
 *     rationaleTitle = "权限说明",
 *     rationaleMessage = "我们需要相机和麦克风权限来录制视频。"
 * )
 * ```
 *
 * 注意事项：
 * - 必须在主线程调用
 * - 一个 PermissionHelper 实例同时只能有一个挂起的权限请求
 * - 如果用户取消请求（如点击空白区域），返回的 Map 中对应权限值为 false
 */
class PermissionHelper(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "PermissionHelper"
    }

    /**
     * 权限请求启动器
     *
     * 通过 registerForActivityResult 注册，处理权限请求结果。
     */
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    /**
     * 挂起函数继续点
     *
     * 用于在权限请求完成后恢复协程。
     */
    @Volatile
    private var continuation: kotlinx.coroutines.CancellableContinuation<Map<String, Boolean>>? = null

    init {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            Logger.d(TAG, "权限请求结果: $result")
            continuation?.let { cont ->
                if (cont.isActive) {
                    cont.resume(result)
                }
            }
            continuation = null
        }
    }

    /**
     * 请求单个权限（挂起函数）
     *
     * 如果权限已授予，直接返回 true，不弹窗。
     * 否则弹出系统权限请求对话框，等待用户操作后返回结果。
     *
     * @param permission 权限名称（如 Manifest.permission.CAMERA）
     * @return true=权限已授予，false=权限被拒绝
     */
    suspend fun requestPermission(permission: String): Boolean {
        // 检查是否已授予
        if (PermissionUtil.hasPermission(activity, permission)) {
            Logger.d(TAG, "权限已授予: $permission")
            return true
        }

        // 弹出权限请求
        val result = requestPermissions(permission)
        return result[permission] ?: false
    }

    /**
     * 请求多个权限（挂起函数）
     *
     * 自动过滤已授予的权限，仅请求未授予的权限。
     * 如果所有权限已授予，直接返回全部 true。
     * 申请完成后自动通过 [PermissionResultBus] 广播结果。
     *
     * @param permissions 权限名称数组
     * @return Map<权限名称, 是否授予>
     */
    suspend fun requestPermissions(vararg permissions: String): Map<String, Boolean> {
        // 分离已授予和未授予的权限
        val grantedPermissions = mutableMapOf<String, Boolean>()
        val needRequestPermissions = mutableListOf<String>()

        for (permission in permissions) {
            if (PermissionUtil.hasPermission(activity, permission)) {
                grantedPermissions[permission] = true
            } else {
                needRequestPermissions.add(permission)
            }
        }

        // 所有权限已授予，直接返回
        if (needRequestPermissions.isEmpty()) {
            Logger.d(TAG, "所有权限已授予: ${permissions.toList()}")
            return grantedPermissions
        }

        // 弹出权限请求对话框
        val result = suspendCancellableCoroutine { cont ->
            continuation = cont

            cont.invokeOnCancellation {
                continuation = null
                Logger.w(TAG, "权限请求被取消")
            }

            Logger.d(TAG, "请求权限: $needRequestPermissions")
            permissionLauncher.launch(needRequestPermissions.toTypedArray())
        }

        // 合并已授予的权限结果
        grantedPermissions.putAll(result)

        // 检测永久拒绝状态并广播到结果总线
        val permanentDenials = needRequestPermissions.filter { perm ->
            result[perm] == false && !activity.shouldShowRequestPermissionRationale(perm)
        }
        val hasPermanentDenial = permanentDenials.isNotEmpty()

        if (hasPermanentDenial) {
            Logger.w(TAG, "以下权限被永久拒绝（用户勾选了'不再询问'）: $permanentDenials")
        }

        // 通过结果总线广播
        val permissionResult = PermissionResult(
            permissions = permissions.toList(),
            grantResults = grantedPermissions,
            allGranted = grantedPermissions.values.all { it },
            hasPermanentDenial = hasPermanentDenial
        )
        PermissionResultBus.sendResult(permissionResult)

        return grantedPermissions
    }

    /**
     * 带智能 Rationale 处理的权限请求（挂起函数）
     *
     * 智能处理权限请求的完整流程：
     * 1. 已授予 → 直接返回 true
     * 2. `shouldShowRequestPermissionRationale` 为 true → 先弹解释弹窗，用户确认后再申请
     * 3. 首次申请 → 直接弹系统权限申请
     * 4. 被拒绝且 `shouldShowRequestPermissionRationale` 为 false（永久拒绝）→ 弹引导去设置页
     *
     * 这是 [requestPermissionWithRationale] 的简化版本，适合大多数场景使用。
     *
     * 用法示例：
     * ```kotlin
     * val granted = permissionHelper.requestWithRationale(
     *     permission = Manifest.permission.CAMERA,
     *     rationaleMessage = "我们需要相机权限来拍摄照片"
     * )
     * ```
     *
     * @param permission 权限名称（如 Manifest.permission.CAMERA）
     * @param rationaleMessage 权限用途解释文案
     * @param rationaleTitle 解释弹窗标题（默认"权限申请"）
     * @return true=权限已授予，false=权限被拒绝
     */
    suspend fun requestWithRationale(
        permission: String,
        rationaleMessage: String,
        rationaleTitle: String = "权限申请"
    ): Boolean {
        return requestPermissionWithRationale(
            permission = permission,
            rationaleTitle = rationaleTitle,
            rationaleMessage = rationaleMessage
        )
    }

    /**
     * 带智能 Rationale 处理的多权限请求（挂起函数）
     *
     * 批量申请权限的智能版本，自动处理 rationale 弹窗和永久拒绝引导。
     *
     * 用法示例：
     * ```kotlin
     * val results = permissionHelper.requestWithRationale(
     *     permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
     *     rationaleMessage = "我们需要相机和麦克风权限来录制视频"
     * )
     * ```
     *
     * @param permissions 权限名称数组
     * @param rationaleMessage 权限用途解释文案
     * @param rationaleTitle 解释弹窗标题（默认"权限申请"）
     * @return Map<权限名称, 是否授予>
     */
    suspend fun requestWithRationale(
        permissions: Array<String>,
        rationaleMessage: String,
        rationaleTitle: String = "权限申请"
    ): Map<String, Boolean> {
        return requestPermissionsWithRationale(
            permissions = permissions,
            rationaleTitle = rationaleTitle,
            rationaleMessage = rationaleMessage
        )
    }

    /**
     * 带 Rationale 解释弹窗的单权限申请（挂起函数）
     *
     * 在申请危险权限前，先检查是否需要显示解释弹窗（shouldShowRequestPermissionRationale）。
     * 如果需要，则展示一个 Material 对话框向用户说明权限用途，用户点击"确认"后再发起系统权限申请。
     * 如果权限已被授予，直接返回 true。
     * 如果权限被永久拒绝（!shouldShowRequestPermissionRationale 且未授予），
     * 则弹窗引导用户前往系统设置页手动开启。
     *
     * @param permission 权限名称
     * @param rationaleTitle 解释弹窗标题
     * @param rationaleMessage 解释弹窗正文
     * @param confirmButtonText 确认按钮文字（默认"去开启"）
     * @param cancelButtonText 取消按钮文字（默认"取消"）
     * @return true=权限已授予，false=权限被拒绝
     */
    suspend fun requestPermissionWithRationale(
        permission: String,
        rationaleTitle: String,
        rationaleMessage: String,
        confirmButtonText: String = "去开启",
        cancelButtonText: String = "取消"
    ): Boolean {
        // 已授予，直接返回
        if (PermissionUtil.hasPermission(activity, permission)) {
            return true
        }

        // 检查是否被永久拒绝（!shouldShowRequestPermissionRationale 且未授予）
        if (!activity.shouldShowRequestPermissionRationale(permission)) {
            // 首次申请或永久拒绝：
            // 如果是首次，直接弹系统权限弹窗；如果是永久拒绝，引导去设置页
            val isFirstRequest = isFirstTimeRequest(permission)
            if (!isFirstRequest) {
                // 永久拒绝 → 引导去设置页
                val goSettings = showSettingsDialog(
                    title = "权限已被永久拒绝",
                    message = "$permission 权限已被永久拒绝，请前往系统设置手动开启。",
                    confirmText = "去设置",
                    cancelText = cancelButtonText
                )
                if (goSettings) {
                    openAppSettings()
                }
                return false
            }
        }

        // 需要显示 Rationale（用户之前拒绝过但未勾选"不再询问"）
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            val confirmed = showRationaleDialog(
                title = rationaleTitle,
                message = rationaleMessage,
                confirmText = confirmButtonText,
                cancelText = cancelButtonText
            )
            if (!confirmed) {
                return false
            }
        }

        // 用户确认后发起系统权限申请
        val result = requestPermissions(permission)
        val granted = result[permission] ?: false

        // 申请被拒后，检查是否被永久拒绝，引导去设置页
        if (!granted && !activity.shouldShowRequestPermissionRationale(permission)) {
            val goSettings = showSettingsDialog(
                title = "权限申请被拒绝",
                message = "$permission 权限被拒绝，如需使用该功能请前往系统设置手动开启。",
                confirmText = "去设置",
                cancelText = cancelButtonText
            )
            if (goSettings) {
                openAppSettings()
            }
        }

        return granted
    }

    /**
     * 带 Rationale 解释弹窗的多权限申请（挂起函数）
     *
     * 批量申请权限前先展示一个统一的解释弹窗，用户确认后发起系统权限申请。
     * 如果有权限被永久拒绝，弹窗引导用户前往系统设置页。
     *
     * @param permissions 权限名称数组
     * @param rationaleTitle 解释弹窗标题
     * @param rationaleMessage 解释弹窗正文
     * @param confirmButtonText 确认按钮文字
     * @param cancelButtonText 取消按钮文字
     * @return Map<权限名称, 是否授予>
     */
    suspend fun requestPermissionsWithRationale(
        permissions: Array<String>,
        rationaleTitle: String,
        rationaleMessage: String,
        confirmButtonText: String = "去开启",
        cancelButtonText: String = "取消"
    ): Map<String, Boolean> {
        // 过滤出需要申请的权限
        val needRequest = permissions.filter { !PermissionUtil.hasPermission(activity, it) }
        if (needRequest.isEmpty()) {
            return permissions.associateWith { true }
        }

        // 检查是否需要显示 Rationale（至少有一个权限需要解释）
        val needsRationale = needRequest.any { activity.shouldShowRequestPermissionRationale(it) }
        if (needsRationale) {
            val confirmed = showRationaleDialog(
                title = rationaleTitle,
                message = rationaleMessage,
                confirmText = confirmButtonText,
                cancelText = cancelButtonText
            )
            if (!confirmed) {
                return permissions.associateWith { perm ->
                    PermissionUtil.hasPermission(activity, perm)
                }
            }
        }

        // 发起系统权限申请
        val result = requestPermissions(*permissions)

        // 检查是否有永久拒绝的权限
        val permanentlyDenied = needRequest.filter { perm ->
            result[perm] == false && !activity.shouldShowRequestPermissionRationale(perm)
        }
        if (permanentlyDenied.isNotEmpty()) {
            val goSettings = showSettingsDialog(
                title = "部分权限被永久拒绝",
                message = "以下权限已被永久拒绝，请前往系统设置手动开启：\n${permanentlyDenied.joinToString("\n")}",
                confirmText = "去设置",
                cancelText = cancelButtonText
            )
            if (goSettings) {
                openAppSettings()
            }
        }

        return result
    }

    /**
     * 显示 Rationale 解释弹窗（挂起函数）
     *
     * 使用 MaterialAlertDialog 向用户解释为什么需要该权限，
     * 用户点击"确认"后返回 true，点击"取消"返回 false。
     *
     * @param title 弹窗标题
     * @param message 弹窗正文
     * @param confirmText 确认按钮文字
     * @param cancelText 取消按钮文字
     * @return true=用户点击确认，false=用户点击取消
     */
    private suspend fun showRationaleDialog(
        title: String,
        message: String,
        confirmText: String,
        cancelText: String
    ): Boolean = suspendCancellableCoroutine { cont ->
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(confirmText) { dialog, _ ->
                dialog.dismiss()
                if (cont.isActive) cont.resume(true)
            }
            .setNegativeButton(cancelText) { dialog, _ ->
                dialog.dismiss()
                if (cont.isActive) cont.resume(false)
            }
            .setOnCancelListener {
                if (cont.isActive) cont.resume(false)
            }
            .show()

        cont.invokeOnCancellation {
            Logger.w(TAG, "Rationale 弹窗被取消")
        }
    }

    /**
     * 显示引导去设置页的弹窗（挂起函数）
     *
     * 当权限被永久拒绝时，弹窗告知用户并提供"去设置"按钮，
     * 点击后返回 true（调用方应调用 [openAppSettings] 跳转）。
     *
     * @param title 弹窗标题
     * @param message 弹窗正文
     * @param confirmText 确认按钮文字
     * @param cancelText 取消按钮文字
     * @return true=用户点击"去设置"，false=用户点击取消
     */
    private suspend fun showSettingsDialog(
        title: String,
        message: String,
        confirmText: String,
        cancelText: String
    ): Boolean = suspendCancellableCoroutine { cont ->
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(confirmText) { dialog, _ ->
                dialog.dismiss()
                if (cont.isActive) cont.resume(true)
            }
            .setNegativeButton(cancelText) { dialog, _ ->
                dialog.dismiss()
                if (cont.isActive) cont.resume(false)
            }
            .setOnCancelListener {
                if (cont.isActive) cont.resume(false)
            }
            .show()
    }

    /**
     * 打开当前应用的系统设置页
     *
     * 引导用户手动开启被永久拒绝的权限。
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
        Logger.d(TAG, "已跳转到应用设置页")
    }

    /**
     * 检查是否应该显示权限说明（用户之前拒绝过）
     *
     * @param permission 权限名称
     * @return true=应该显示说明，false=不需要
     */
    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * 判断是否为首次申请某权限
     *
     * 通过 shouldShowRequestPermissionRationale 反向判断：
     * - 返回 false 且权限未授予 → 可能是首次申请或已被永久拒绝
     * - 这里用一个简单的 SharedPreferences 标记来区分
     *
     * @param permission 权限名称
     * @return true=首次申请，false=非首次
     */
    private fun isFirstTimeRequest(permission: String): Boolean {
        val prefs = activity.getSharedPreferences("permission_helper", Context.MODE_PRIVATE)
        val hasRequestedBefore = prefs.getBoolean("requested_$permission", false)
        if (!hasRequestedBefore) {
            prefs.edit().putBoolean("requested_$permission", true).apply()
            return true
        }
        return false
    }
}
