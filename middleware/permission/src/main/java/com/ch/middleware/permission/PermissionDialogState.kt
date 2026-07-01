package com.ch.middleware.permission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 权限弹窗数据类
 *
 * 描述一个待显示的权限说明弹窗。
 *
 * @property title 弹窗标题
 * @property message 弹窗正文
 * @property confirmText 确认按钮文案
 * @property cancelText 取消按钮文案
 */
data class PermissionDialogRequest(
    val title: String,
    val message: String,
    val confirmText: String,
    val cancelText: String,
)

/**
 * 权限弹窗状态桥
 *
 * 将 PermissionHelper 的弹窗请求以状态形式暴露，
 * 由 app 层 Compose 树观察并渲染 [com.ch.core.ui.component.GlobalDialog]。
 *
 * 设计说明：
 * - middleware 模块不依赖 Compose，仅通过 [StateFlow] 暴露弹窗请求
 * - app 层在 Compose 树中观察 [pendingDialog]，有值时显示 GlobalDialog
 * - 用户操作后调用 [resolve] 回传结果，PermissionHelper 的挂起协程自动恢复
 *
 * 用法示例（在 Activity 的 Compose 内容中）：
 * ```kotlin
 * PermissionDialogHost()
 * ```
 */
object PermissionDialogState {

    /**
     * 当前待处理的弹窗请求
     *
     * null 表示无弹窗。非 null 时 app 层应显示 GlobalDialog。
     */
    private val _pendingDialog = MutableStateFlow<PermissionDialogRequest?>(null)
    val pendingDialog: StateFlow<PermissionDialogRequest?> = _pendingDialog.asStateFlow()

    /**
     * 弹窗结果回调
     *
     * 由 PermissionHelper 内部设置，app 层无需直接操作。
     */
    @Volatile
    internal var onResult: ((Boolean) -> Unit)? = null

    /**
     * 发起弹窗请求（由 PermissionHelper 内部调用）
     *
     * 设置弹窗状态并等待用户操作结果。
     *
     * @param request 弹窗请求
     * @return true=用户点击确认，false=用户点击取消
     */
    internal suspend fun requestDialog(request: PermissionDialogRequest): Boolean {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            onResult = { result ->
                if (cont.isActive) {
                    cont.resume(result, null)
                }
                onResult = null
            }
            _pendingDialog.value = request

            cont.invokeOnCancellation {
                onResult = null
                _pendingDialog.value = null
            }
        }
    }

    /**
     * 用户操作完成，回传结果并清除弹窗状态
     *
     * 由 app 层在 GlobalDialog 的 onConfirm/onDismiss 中调用。
     *
     * @param confirmed true=确认，false=取消
     */
    fun resolve(confirmed: Boolean) {
        _pendingDialog.value = null
        onResult?.invoke(confirmed)
        onResult = null
    }
}
