package com.ch.core.network.client

import android.content.Context
import com.ch.core.network.BuildConfig
import com.ch.core.network.interceptor.AuthInterceptor
import com.ch.core.network.interceptor.CacheInterceptor
import com.ch.core.network.interceptor.LogInterceptor
import com.ch.core.network.interceptor.MonitorInterceptor
import com.ch.core.network.interceptor.RetryInterceptor
import com.ch.core.network.interceptor.SecurityInterceptor
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 网络客户端单例（商业级增强版）
 *
 * 封装 OkHttpClient 和 Retrofit 的创建与配置。
 * 内置缓存、重试、日志、监控、SSL Pinning 拦截器。
 *
 * 使用前必须调用 [init] 初始化：
 * ```kotlin
 * class SampleApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         NetworkClient.init(this, "https://api.example.com/")
 *     }
 * }
 * ```
 *
 * 使用示例：
 * ```kotlin
 * // 创建 API 服务
 * val userService = NetworkClient.create(UserApi::class.java)
 *
 * // 获取自定义 baseUrl 的 Retrofit
 * val customRetrofit = NetworkClient.getRetrofit("https://other-api.com/")
 *
 * // 取消所有请求
 * NetworkClient.getOkHttpClient().dispatcher.cancelAll()
 * ```
 */
object NetworkClient {

    /**
     * 默认 Base URL
     */
    private const val DEFAULT_BASE_URL = "https://api.example.com/"

    /**
     * 缓存大小：20MB
     */
    private const val CACHE_SIZE = 20L * 1024 * 1024

    /**
     * 连接超时时间（秒）
     */
    private const val CONNECT_TIMEOUT = 30L

    /**
     * 读取超时时间（秒）
     */
    private const val READ_TIMEOUT = 30L

    /**
     * 写入超时时间（秒）
     */
    private const val WRITE_TIMEOUT = 30L

    /**
     * 缓存目录
     */
    private lateinit var cacheDir: File

    /**
     * Application Context（用于 SSL Pinning 配置）
     */
    private lateinit var appContext: Context

    /**
     * OkHttpClient 实例
     */
    private var okHttpClient: OkHttpClient? = null

    /**
     * Retrofit 实例
     */
    private var retrofit: Retrofit? = null

    /**
     * SSL Pinning 域名与证书哈希映射
     *
     * 格式：域名 -> SHA-256 证书哈希列表
     *
     * 注意：
     * - 正式环境必须绑定至少两个证书（主备），防止证书轮换导致服务中断
     * - 哈希值必须替换为真实证书的 SHA-256 指纹
     * - 获取方式：openssl s_client -connect your-api-domain.com:443 | openssl x509 -noout -fingerprint -sha256
     *
     * 示例：
     * ```kotlin
     * private val SSL_PINS = mapOf(
     *     "api.example.com" to listOf(
     *         "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",  // 主证书
     *         "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="   // 备用证书
     *     )
     * )
     * ```
     */
    private val SSL_PINS: Map<String, List<String>> = mapOf(
        // TODO: 替换为真实的域名和证书哈希
        // "your-api-domain.com" to listOf(
        //     "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",  // 主证书
        //     "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="   // 备用证书
        // )
    )

    /**
     * 初始化网络客户端
     *
     * 必须在 Application.onCreate() 中调用。
     *
     * @param context Application Context
     * @param baseUrl 默认 Base URL（可选）
     */
    fun init(context: Context, baseUrl: String = DEFAULT_BASE_URL) {
        appContext = context.applicationContext
        cacheDir = File(context.cacheDir, "http_cache")
        SecurityInterceptor.init(context.applicationContext)
        retrofit = buildRetrofit(baseUrl)
    }

    /**
     * 获取 OkHttpClient 实例
     *
     * @return OkHttpClient
     */
    fun getOkHttpClient(): OkHttpClient {
        return okHttpClient ?: buildOkHttpClient().also { okHttpClient = it }
    }

    /**
     * 获取 Retrofit 实例
     *
     * @return Retrofit
     * @throws IllegalStateException 如果未调用 init() 初始化
     */
    fun getRetrofit(): Retrofit {
        return retrofit ?: throw IllegalStateException(
            "NetworkClient not initialized. Call NetworkClient.init(context) first."
        )
    }

    /**
     * 获取自定义 baseUrl 的 Retrofit 实例
     *
     * @param baseUrl Base URL
     * @return Retrofit
     */
    fun getRetrofit(baseUrl: String): Retrofit {
        return buildRetrofit(baseUrl)
    }

    /**
     * 创建 API 服务实例
     *
     * @param T API 接口类型
     * @param service API 接口 Class
     * @return API 服务实例
     */
    fun <T> create(service: Class<T>): T {
        return getRetrofit().create(service)
    }

    /**
     * 构建 CertificatePinner
     *
     * 仅在非 DEBUG 模式下启用 SSL Pinning。
     * DEBUG 模式下返回 null（不固定证书），便于调试和抓包。
     *
     * @return CertificatePinner 或 null
     */
    private fun buildCertificatePinner(): CertificatePinner? {
        // Debug 模式下绕过 SSL Pinning，便于 Charles/Fiddler 抓包调试
        if (BuildConfig.DEBUG) {
            return null
        }

        if (SSL_PINS.isEmpty()) {
            return null
        }

        val builder = CertificatePinner.Builder()
        SSL_PINS.forEach { (domain, pins) ->
            pins.forEach { pin ->
                builder.add(domain, pin)
            }
        }
        return builder.build()
    }

    /**
     * 构建 OkHttpClient
     *
     * 拦截器执行顺序（从上到下）：
     * 1. MonitorInterceptor - 网络监控（最外层，记录完整耗时）
     * 2. CacheInterceptor - 缓存策略
     * 3. RetryInterceptor - 重试机制
     * 4. LogInterceptor - 日志打印（仅 DEBUG）
     *
     * @return OkHttpClient
     */
    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, CACHE_SIZE))
            // 拦截器按添加顺序执行
            .addInterceptor(MonitorInterceptor())
            .addInterceptor(SecurityInterceptor())
            .addInterceptor(AuthInterceptor()) // 认证拦截器（自动添加 Token、处理 401 刷新）
            .addInterceptor(CacheInterceptor())
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(LogInterceptor())

        // SSL Pinning（仅 Release 模式）
        val certificatePinner = buildCertificatePinner()
        if (certificatePinner != null) {
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }

    /**
     * 构建 Retrofit
     *
     * @param baseUrl Base URL
     * @return Retrofit
     */
    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
