package com.ch.core.common.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Proxy

/**
 * 网络状态工具类
 *
 * 提供网络连接状态检测、WiFi/移动网络判断、代理检测等功能。
 * 所有方法内部捕获 SecurityException，异常时返回 false。
 *
 * 注意：需要在 AndroidManifest.xml 中声明以下权限：
 * ```xml
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * ```
 */
object NetworkUtil {

    /**
     * 判断网络是否已连接
     *
     * @param context 上下文
     * @return true 表示已连接且可用
     */
    fun isConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 判断当前是否为 WiFi 连接
     *
     * @param context 上下文
     * @return true 表示当前通过 WiFi 联网
     */
    fun isWifi(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 判断当前是否为移动数据连接
     *
     * @param context 上下文
     * @return true 表示当前通过移动网络（蜂窝）联网
     */
    fun isMobile(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检测是否开启了系统代理
     *
     * 通过读取 [Proxy.getDefaultHost] 判断，适用于部分抓包/代理场景检测。
     *
     * @param context 上下文
     * @return true 表示检测到代理配置
     */
    fun isProxyEnable(context: Context): Boolean {
        return try {
            val host = Proxy.getDefaultHost()
            val port = Proxy.getDefaultPort()
            !host.isNullOrEmpty() && port > 0
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
