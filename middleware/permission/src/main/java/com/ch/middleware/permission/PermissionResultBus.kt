package com.ch.middleware.permission

import com.ch.core.common.logger.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 权限申请结果数据类
 *
 * 封装一次权限申请的完整结果，包括请求的权限列表和每个权限的授予状态。
 *
 * @property permissions 本次请求的权限列表
 * @property grantResults 每个权限的授予结果（权限名 → 是否授予）
 * @property allGranted 是否所有权限都已授予
 * @property hasPermanentDenial 是否有权限被永久拒绝（用户勾选了"不再询问"）
 *
 * 使用示例：
 * ```kotlin
 * val result = PermissionResult(
 *     permissions = listOf(Manifest.permission.CAMERA),
 *     grantResults = mapOf(Manifest.permission.CAMERA to false),
 *     allGranted = false,
 *     hasPermanentDenial = true
 * )
 * ```
 */
data class PermissionResult(
    val permissions: List<String>,
    val grantResults: Map<String, Boolean>,
    val allGranted: Boolean,
    val hasPermanentDenial: Boolean
)

/**
 * 权限申请结果总线（单例）
 *
 * 基于 [MutableSharedFlow] 实现的全局事件总线，用于在权限申请完成后
 * 将结果分发给所有订阅者（Activity、Fragment、Dialog 等）。
 *
 * **核心特性**：
 * - 使用 [MutableSharedFlow] 保证多订阅者并发安全
 * - 提供 [sendResult] 发送权限结果
 * - 提供 [collectResult] 收集权限结果（返回冷 Flow）
 * - 线程安全，可在任意线程发送和收集
 *
 * **使用场景**：
 * - 在 Activity 或 Fragment 中自动订阅，权限结果返回时自动分发
 * - 在 Dialog 或自定义组件中订阅，响应权限结果更新 UI
 * - 跨组件通信：权限申请在 A 组件发起，B 组件响应结果
 *
 * **使用示例**：
 * ```kotlin
 * // 在 Activity 中订阅
 * lifecycleScope.launch {
 *     PermissionResultBus.collectResult().collect { result ->
 *         if (result.allGranted) {
 *             // 所有权限已授予，执行操作
 *         } else if (result.hasPermanentDenial) {
 *             // 有权限被永久拒绝，引导用户去设置页
 *         }
 *     }
 * }
 *
 * // 在 PermissionHelper 中发送结果
 * PermissionResultBus.sendResult(permissionResult)
 * ```
 *
 * @see PermissionResult
 */
object PermissionResultBus {

    /**
     * 内部共享流，缓冲区大小 64，额外溢出时挂起
     *
     * replay = 0：不重放历史事件，新订阅者只能收到订阅后的事件
     * extraBufferCapacity = 64：缓冲 64 个事件，避免发送者被挂起
     */
    private val _results = MutableSharedFlow<PermissionResult>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /**
     * 对外暴露的只读 Flow
     *
     * 调用方通过 [collectResult] 获取只读 Flow，无法向总线发送事件。
     */
    private val results = _results.asSharedFlow()

    /**
     * 发送权限申请结果
     *
     * 在权限申请完成后调用，将结果广播给所有订阅者。
     * 使用 tryEmit 非阻塞发送，不会挂起调用方。
     *
     * @param result 权限申请结果
     */
    fun sendResult(result: PermissionResult) {
        val emitted = _results.tryEmit(result)
        if (!emitted) {
            Logger.w("PermissionResultBus", "发送权限结果失败：缓冲区已满")
        }
    }

    /**
     * 收集权限申请结果
     *
     * 返回冷 Flow，调用方在协程中 collect 即可接收权限结果事件。
     * 建议在 [kotlinx.coroutines.Main] 线程收集，以便直接更新 UI。
     *
     * @return 权限结果 Flow
     */
    fun collectResult(): Flow<PermissionResult> = results
}
