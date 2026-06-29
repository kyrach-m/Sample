package com.ch.middleware.permission

import com.ch.core.common.logger.Logger
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 权限委托助手（组合模式）
 *
 * 为 Activity 提供权限请求能力，无需继承特定基类。
 * 通过组合模式实现功能复用，保持架构灵活性。
 *
 * 核心特性：
 * - 无需继承，任何 Activity 都可通过委托获得权限能力
 * - 自动绑定 Activity 生命周期，销毁时自动清理资源
 * - 内置 [PermissionHelper] 和协程作用域
 * - 自动订阅 [PermissionResultBus] 接收权限结果
 *
 * 使用示例：
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *
 *     // 创建权限委托
 *     private val permissionDelegate by lazy {
 *         PermissionDelegate(this).apply {
 *             onPermissionResult = { result ->
 *                 if (result.allGranted) {
 *                     // 所有权限已授予
 *                 }
 *             }
 *         }
 *     }
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         // 初始化委托（绑定生命周期）
 *         permissionDelegate.onCreate()
 *
 *         // 请求权限
 *         permissionDelegate.requestPermissionAsync(Manifest.permission.CAMERA) { granted ->
 *             if (granted) openCamera()
 *         }
 *     }
 * }
 * ```
 *
 * 与继承方式的对比：
 * - ✅ 不污染基类，保持架构纯净
 * - ✅ 按需使用，不需要的 Activity 不受影响
 * - ✅ 可与其他基类组合使用
 * - ✅ 易于测试和替换
 *
 * @param activity 绑定的 Activity
 * @see PermissionHelper
 * @see PermissionResultBus
 */
class PermissionDelegate(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "PermissionDelegate"
    }

    /**
     * 权限请求助手
     */
    val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(activity)
    }

    /**
     * 权限请求协程作用域
     *
     * 绑定到 Activity 生命周期，Activity 销毁时自动取消。
     */
    private val permissionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * PermissionResultBus 订阅 Job
     */
    private var resultBusJob: Job? = null

    /**
     * 权限结果回调
     *
     * 当 [PermissionResultBus] 广播权限结果时触发。
     * 可在外部设置自定义处理逻辑。
     *
     * @see PermissionResult
     */
    var onPermissionResult: ((PermissionResult) -> Unit)? = null

    /**
     * 是否已初始化
     */
    private var isInitialized = false

    /**
     * 初始化委托
     *
     * 必须在 Activity 的 onCreate 中调用。
     * 注册生命周期观察，订阅权限结果总线。
     */
    fun onCreate() {
        if (isInitialized) {
            Logger.w(TAG, "PermissionDelegate 已初始化，请勿重复调用")
            return
        }
        isInitialized = true

        // 注册生命周期观察
        activity.lifecycle.addObserver(this)

        // 订阅权限结果总线
        subscribePermissionResultBus()

        Logger.d(TAG, "${activity.javaClass.simpleName} PermissionDelegate 已初始化")
    }

    /**
     * 订阅权限结果总线
     */
    private fun subscribePermissionResultBus() {
        resultBusJob = permissionScope.launch {
            PermissionResultBus.collectResult().collect { result ->
                Logger.d(TAG, "收到权限结果: allGranted=${result.allGranted}, hasPermanentDenial=${result.hasPermanentDenial}")
                onPermissionResult?.invoke(result)
            }
        }
    }

    /**
     * 请求单个权限（异步回调方式）
     *
     * @param permission 权限名称
     * @param callback 结果回调，true=已授予，false=已拒绝
     */
    fun requestPermissionAsync(permission: String, callback: (Boolean) -> Unit) {
        permissionScope.launch {
            val granted = permissionHelper.requestPermission(permission)
            callback(granted)
        }
    }

    /**
     * 请求多个权限（异步回调方式）
     *
     * @param permissions 权限名称数组
     * @param callback 结果回调，Map<权限名称, 是否授予>
     */
    fun requestPermissionsAsync(vararg permissions: String, callback: (Map<String, Boolean>) -> Unit) {
        permissionScope.launch {
            val result = permissionHelper.requestPermissions(*permissions)
            callback(result)
        }
    }

    /**
     * 带 Rationale 解释弹窗的单权限申请（异步回调方式）
     *
     * @param permission 权限名称
     * @param rationaleTitle 解释弹窗标题
     * @param rationaleMessage 解释弹窗正文
     * @param callback 结果回调，true=已授予，false=已拒绝
     */
    fun requestPermissionWithRationaleAsync(
        permission: String,
        rationaleTitle: String,
        rationaleMessage: String,
        callback: (Boolean) -> Unit
    ) {
        permissionScope.launch {
            val granted = permissionHelper.requestPermissionWithRationale(
                permission = permission,
                rationaleTitle = rationaleTitle,
                rationaleMessage = rationaleMessage
            )
            callback(granted)
        }
    }

    /**
     * 带 Rationale 解释弹窗的多权限申请（异步回调方式）
     *
     * @param permissions 权限名称数组
     * @param rationaleTitle 解释弹窗标题
     * @param rationaleMessage 解释弹窗正文
     * @param callback 结果回调，Map<权限名称, 是否授予>
     */
    fun requestPermissionsWithRationaleAsync(
        permissions: Array<String>,
        rationaleTitle: String,
        rationaleMessage: String,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        permissionScope.launch {
            val result = permissionHelper.requestPermissionsWithRationale(
                permissions = permissions,
                rationaleTitle = rationaleTitle,
                rationaleMessage = rationaleMessage
            )
            callback(result)
        }
    }

    /**
     * 请求单个权限（挂起函数方式）
     *
     * 必须在协程中调用。
     *
     * @param permission 权限名称
     * @return true=已授予，false=已拒绝
     */
    suspend fun requestPermission(permission: String): Boolean {
        return permissionHelper.requestPermission(permission)
    }

    /**
     * 请求多个权限（挂起函数方式）
     *
     * 必须在协程中调用。
     *
     * @param permissions 权限名称数组
     * @return Map<权限名称, 是否授予>
     */
    suspend fun requestPermissions(vararg permissions: String): Map<String, Boolean> {
        return permissionHelper.requestPermissions(*permissions)
    }

    /**
     * 带 Rationale 解释弹窗的单权限申请（挂起函数方式）
     *
     * 必须在协程中调用。
     *
     * @param permission 权限名称
     * @param rationaleTitle 解释弹窗标题
     * @param rationaleMessage 解释弹窗正文
     * @return true=已授予，false=已拒绝
     */
    suspend fun requestPermissionWithRationale(
        permission: String,
        rationaleTitle: String,
        rationaleMessage: String
    ): Boolean {
        return permissionHelper.requestPermissionWithRationale(
            permission = permission,
            rationaleTitle = rationaleTitle,
            rationaleMessage = rationaleMessage
        )
    }

    /**
     * 带 Rationale 解释弹窗的多权限申请（挂起函数方式）
     *
     * 必须在协程中调用。
     *
     * @param permissions 权限名称数组
     * @param rationaleTitle 解释弹窗标题
     * @param rationaleMessage 解释弹窗正文
     * @return Map<权限名称, 是否授予>
     */
    suspend fun requestPermissionsWithRationale(
        permissions: Array<String>,
        rationaleTitle: String,
        rationaleMessage: String
    ): Map<String, Boolean> {
        return permissionHelper.requestPermissionsWithRationale(
            permissions = permissions,
            rationaleTitle = rationaleTitle,
            rationaleMessage = rationaleMessage
        )
    }

    /**
     * 检查权限是否已授予
     *
     * @param permission 权限名称
     * @return true=已授予，false=未授予
     */
    fun isPermissionGranted(permission: String): Boolean {
        return permissionHelper.isPermissionGranted(activity, permission)
    }

    /**
     * 检查多个权限是否全部已授予
     *
     * @param permissions 权限名称数组
     * @return true=全部已授予
     */
    fun isAllPermissionsGranted(vararg permissions: String): Boolean {
        return permissionHelper.isAllPermissionsGranted(activity, *permissions)
    }

    /**
     * 打开应用系统设置页
     *
     * 用于引导用户手动开启被永久拒绝的权限。
     */
    fun openAppSettings() {
        permissionHelper.openAppSettings()
    }

    /**
     * Lifecycle 销毁回调
     *
     * Activity 销毁时自动清理资源。
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // 取消权限结果总线订阅
        resultBusJob?.cancel()
        resultBusJob = null
        // 取消所有权限请求协程
        permissionScope.cancel()
        Logger.d(TAG, "${activity.javaClass.simpleName} PermissionDelegate 已销毁")
    }
}
