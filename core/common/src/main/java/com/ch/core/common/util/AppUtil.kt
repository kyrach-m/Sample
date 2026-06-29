package com.ch.core.common.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 应用信息工具类
 *
 * 提供获取应用版本号、包名、渠道信息等常用方法。
 */
object AppUtil {

    /**
     * 获取应用版本名称（versionName）
     *
     * @param context 上下文
     * @return 版本名称，如 "1.0.0"，获取失败返回 ""
     */
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取应用版本号（versionCode）
     *
     * 兼容 Android O 及以上和旧版本
     *
     * @param context 上下文
     * @return 版本号，获取失败返回 0
     */
    fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取应用包名
     *
     * @param context 上下文
     * @return 包名，如 "com.ch.sample"
     */
    fun getPackageName(context: Context): String {
        return try {
            context.packageName
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取渠道信息
     *
     * 读取 AndroidManifest.xml 中 <meta-data> 的 CHANNEL 字段。
     * 用于多渠道打包时区分来源。
     *
     * 在 AndroidManifest.xml 中配置：
     * ```xml
     * <meta-data android:name="CHANNEL" android:value="huawei" />
     * ```
     *
     * @param context 上下文
     * @return 渠道名称，未配置时默认返回 "official"
     */
    fun getChannel(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("CHANNEL") ?: "official"
        } catch (e: Exception) {
            "official"
        }
    }
}
