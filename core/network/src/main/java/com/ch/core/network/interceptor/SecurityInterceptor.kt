package com.ch.core.network.interceptor

import android.content.Context
import android.provider.Settings
import com.tencent.mmkv.MMKV
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.util.UUID

/**
 * 安全风控拦截器
 *
 * 为所有请求自动附加安全验证 Header：
 * - X-Device-Id：设备唯一标识（首次生成后持久化）
 * - X-Timestamp：当前毫秒时间戳
 * - X-Sign：对 deviceId + timestamp + salt 进行 SHA256 签名
 *
 * 后端通过校验这些 Header 判断请求合法性，防止伪造请求和重放攻击。
 *
 * 使用前必须调用 [init] 初始化：
 * ```kotlin
 * class SampleApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         SecurityInterceptor.init(this)
 *     }
 * }
 * ```
 */
class SecurityInterceptor : Interceptor {

    companion object {
        private const val HEADER_DEVICE_ID = "X-Device-Id"
        private const val HEADER_TIMESTAMP = "X-Timestamp"
        private const val HEADER_SIGN = "X-Sign"

        private const val KEY_DEVICE_ID = "security_device_id"
        private const val MMKV_ID = "security_config"

        /**
         * 签名盐值（混淆存储）
         *
         * 实际项目中应从服务端动态获取或使用更安全的密钥管理方案
         */
        private val SALT: String by lazy {
            val parts = arrayOf("k3y", "_s", "ec", "ur", "e_2026")
            parts.joinToString("")
        }

        /**
         * 设备 ID（延迟初始化）
         */
        @Volatile
        private var deviceId: String? = null

        /**
         * 初始化安全拦截器
         *
         * 必须在 Application.onCreate() 中调用。
         * 生成或恢复设备唯一标识。
         *
         * @param context Application Context
         */
        fun init(context: Context) {
            val mmkv = MMKV.mmkvWithID(MMKV_ID)
            val storedId = mmkv.decodeString(KEY_DEVICE_ID)
            if (storedId != null) {
                deviceId = storedId
            } else {
                // 基于 ANDROID_ID + UUID 生成设备标识
                val androidId = try {
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                } catch (e: Exception) {
                    "unknown"
                }
                val uniqueId = UUID.nameUUIDFromBytes(
                    (androidId + System.currentTimeMillis()).toByteArray()
                ).toString().replace("-", "")
                deviceId = uniqueId
                mmkv.encode(KEY_DEVICE_ID, uniqueId)
            }
        }

        /**
         * 获取设备 ID
         */
        fun getDeviceId(): String {
            return deviceId ?: throw IllegalStateException(
                "SecurityInterceptor not initialized. Call SecurityInterceptor.init(context) first."
            )
        }

        /**
         * SHA256 签名
         *
         * @param data 待签名数据
         * @return 签名结果（十六进制字符串）
         */
        private fun sha256(data: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val timestamp = System.currentTimeMillis().toString()
        val currentDeviceId = getDeviceId()

        // 生成签名：deviceId + timestamp + salt
        val signData = currentDeviceId + timestamp + SALT
        val sign = sha256(signData)

        // 附加安全 Header
        val newRequest = originalRequest.newBuilder()
            .header(HEADER_DEVICE_ID, currentDeviceId)
            .header(HEADER_TIMESTAMP, timestamp)
            .header(HEADER_SIGN, sign)
            .build()

        return chain.proceed(newRequest)
    }
}
