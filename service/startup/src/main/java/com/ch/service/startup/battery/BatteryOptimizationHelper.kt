package com.ch.service.startup.battery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.ch.core.common.logger.Logger
import com.ch.core.common.util.BuildVersion

/**
 * 电池优化检查助手（进程保活工具）
 *
 * 帮助检查和请求电池优化白名单，提升应用后台存活率。
 * 在 Android 6.0 (M) 及以上系统中，默认所有应用都受电池优化限制，
 * 后台服务和网络连接可能会被系统强制停止。
 *
 * 核心功能：
 * - 检查当前应用是否已加入电池优化白名单
 * - 提供跳转电池优化设置页的 Intent
 * - Debug 模式下自动打印警告，提醒开发者检查
 *
 * 使用场景：
 * - Application 启动时检查，Debug 模式下打印警告
 * - 需要后台保活的业务（如音乐播放、运动记录）主动引导用户授权
 * - 推送保活通道初始化前检查
 *
 * 用法示例：
 * ```kotlin
 * // 在 Application.onCreate 中检查
 * if (BuildConfig.DEBUG) {
 *     BatteryOptimizationHelper.checkAndWarn(this)
 * }
 *
 * // 业务层引导用户授权
 * if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
 *     startActivity(BatteryOptimizationHelper.intentToBatterySettings(context))
 * }
 * ```
 *
 * 注意：
 * - 申请电池优化白名单需要引导用户手动操作，无法静默申请
 * - 上架 Google Play 的应用需确保符合 Play 政策，避免恶意保活
 * - 仅在确实需要后台运行的场景下引导用户授权
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    /**
     * 检查当前应用是否已忽略电池优化
     *
     * 在 Android 6.0 (M) 以下，无电池优化限制，直接返回 true。
     *
     * @param context Context
     * @return true=已忽略电池优化（白名单），false=受电池优化限制
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (!BuildVersion.isAtLeastM) {
            return true
        }
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            Logger.e(TAG, "检查电池优化状态失败: ${e.message}")
            false
        }
    }

    /**
     * 检查电池优化状态，Debug 模式下如果未加入白名单则打印警告
     *
     * 建议在 Application.onCreate() 中调用，帮助开发者及时发现保活问题。
     * Release 模式下不执行任何操作。
     *
     * @param context Context
     * @param isDebug 是否为 Debug 模式（通常传 BuildConfig.DEBUG）
     */
    fun checkAndWarn(context: Context, isDebug: Boolean) {
        if (!isDebug) {
            return
        }
        if (!BuildVersion.isAtLeastM) {
            return
        }
        if (!isIgnoringBatteryOptimizations(context)) {
            Logger.w(
                TAG,
                "⚠️ 应用未加入电池优化白名单，后台运行可能受到限制。" +
                    "可通过 BatteryOptimizationHelper.intentToBatterySettings() 跳转设置页"
            )
        } else {
            Logger.d(TAG, "✅ 应用已加入电池优化白名单")
        }
    }

    /**
     * 获取跳转到电池优化设置页的 Intent
     *
     * 打开系统的电池优化白名单设置页面，用户可手动将应用加入白名单。
     *
     * 注意：
     * - 在 Android 6.0 以下系统中无此设置页，返回普通应用设置页 Intent
     * - 部分定制 ROM 可能不支持此 Intent，需做降级处理
     *
     * @param context Context
     * @return 跳转到电池优化设置页的 Intent
     */
    fun intentToBatterySettings(context: Context): Intent {
        return if (BuildVersion.isAtLeastM) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * 获取跳转到电池优化列表页的 Intent
     *
     * 打开系统的"电池优化"列表页，显示所有应用的优化状态。
     * 相比 [intentToBatterySettings] 直接弹窗申请，此方式更温和，
     * 用户可以在列表中找到应用并手动设置。
     *
     * @return 跳转到电池优化列表页的 Intent
     */
    fun intentToBatteryOptimizationList(): Intent {
        return if (BuildVersion.isAtLeastM) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * 判断电池优化申请 Intent 是否可用
     *
     * 部分定制 ROM 可能移除了电池优化申请页面，
     * 调用此方法检查后再决定是否显示引导。
     *
     * @param context Context
     * @return true=系统支持电池优化申请，false=不支持
     */
    fun isBatteryOptimizationIntentAvailable(context: Context): Boolean {
        if (!BuildVersion.isAtLeastM) {
            return false
        }
        return try {
            val intent = intentToBatterySettings(context)
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            Logger.e(TAG, "检查电池优化 Intent 可用性失败: ${e.message}")
            false
        }
    }
}
