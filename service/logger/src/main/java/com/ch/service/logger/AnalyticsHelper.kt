package com.ch.service.logger

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.ch.core.common.logger.Logger
import com.ch.service.logger.db.AnalyticsRepository
import com.ch.service.logger.db.PendingEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 事件上报助手
 *
 * 封装应用内事件上报逻辑，提供统一的事件记录接口。
 * 自动补全 timestamp（时间戳）和 deviceId（设备唯一标识）。
 *
 * 核心特性：
 * - 提供 [logEvent] 方法记录事件
 * - 自动补全 timestamp（毫秒时间戳）
 * - 自动补全 deviceId（设备唯一标识，首先生成后持久化）
 * - 采样率控制（通过 [SampleRateManager]）
 * - 事件落地到 Room 数据库（离线缓存）
 * - 触发 WorkManager 批量上传
 * - 线程安全
 *
 * 使用示例：
 * ```kotlin
 * // 初始化（在 Application.onCreate 中）
 * AnalyticsHelper.init(context)
 *
 * // 记录事件（自动经过采样率判断 + 落地数据库）
 * AnalyticsHelper.logEvent("button_click", mapOf(
 *     "button_name" to "login",
 *     "screen" to "home"
 * ))
 * ```
 *
 * 上报流程：
 * 1. logEvent() → 采样率判断
 * 2. 通过采样 → 写入 Room 数据库（pending_events 表）
 * 3. 触发 AnalyticsUploadWorker 批量上传
 * 4. 上传成功 → 删除本地记录
 * 5. 上传失败 → retryCount++，最多重试 3 次
 */
object AnalyticsHelper {

    private const val TAG = "AnalyticsHelper"

    /**
     * SharedPreferences 文件名
     */
    private const val PREFS_NAME = "analytics_prefs"

    /**
     * 设备 ID 存储 Key
     */
    private const val KEY_DEVICE_ID = "device_id"

    /**
     * 内存事件队列最大容量（备用缓存）
     */
    private const val MAX_QUEUE_SIZE = 1000

    /**
     * 应用 Context
     */
    @SuppressLint("StaticFieldLeak")
    private var appContext: Context? = null

    /**
     * 设备唯一标识
     */
    private var deviceId: String = ""

    /**
     * 内存事件队列（线程安全，作为备用缓存）
     */
    private val eventQueue = ConcurrentLinkedQueue<AnalyticsEvent>()

    /**
     * 是否已初始化
     */
    @Volatile
    private var isInitialized = false

    /**
     * 协程作用域
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 埋点仓库
     */
    @Volatile
    private var repository: AnalyticsRepository? = null

    /**
     * 初始化事件上报助手
     *
     * 必须在 Application.onCreate() 中调用。
     * 初始化时会生成或读取设备 ID，并初始化 Room 数据库。
     *
     * @param context Application Context
     */
    fun init(context: Context) {
        if (isInitialized) {
            Logger.w(TAG, "AnalyticsHelper 已经初始化，忽略重复调用")
            return
        }

        appContext = context.applicationContext
        deviceId = getOrCreateDeviceId(context.applicationContext)
        repository = AnalyticsRepository.getInstance(context.applicationContext)
        isInitialized = true

        Logger.d(TAG, "AnalyticsHelper 初始化完成，deviceId: $deviceId")
    }

    /**
     * 记录事件
     *
     * 执行流程：
     * 1. 采样率判断（通过 [SampleRateManager]）
     * 2. 构建事件（自动补全 timestamp 和 deviceId）
     * 3. 写入 Room 数据库（离线缓存）
     * 4. 同时加入内存队列（备用）
     *
     * @param eventName 事件名称（如 "button_click"、"page_view"）
     * @param params 事件参数（键值对，可选）
     */
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val context = appContext
        if (!isInitialized || context == null) {
            Logger.w(TAG, "AnalyticsHelper 未初始化，事件 $eventName 被丢弃")
            return
        }

        // 采样率判断
        if (!SampleRateManager.shouldLog(eventName)) {
            return
        }

        val timestamp = System.currentTimeMillis()

        // 构建事件
        val event = AnalyticsEvent(
            event = eventName,
            timestamp = timestamp,
            deviceId = deviceId,
            params = params.toMutableMap().apply {
                put("timestamp", timestamp.toString())
                put("deviceId", deviceId)
            }
        )

        // 加入内存队列（备用）
        eventQueue.offer(event)
        while (eventQueue.size > MAX_QUEUE_SIZE) {
            eventQueue.poll()
        }

        // 写入 Room 数据库（离线缓存）
        val entity = PendingEventEntity(
            eventName = eventName,
            params = serializeParams(params),
            timestamp = timestamp,
            deviceId = deviceId
        )

        scope.launch {
            try {
                repository?.save(entity)
                Logger.d(TAG, "事件已落地: $eventName")
            } catch (e: Exception) {
                Logger.e(TAG, "事件落地失败: $eventName", e)
            }
        }

        // Debug 模式下输出日志
        Logger.d(TAG, "事件记录: $eventName | 参数: ${formatParams(params)}")
    }

    /**
     * 获取待上报的事件队列（内存快照）
     *
     * @return 事件列表
     */
    fun getPendingEvents(): List<AnalyticsEvent> {
        return eventQueue.toList()
    }

    /**
     * 清除已上报的事件（内存队列）
     *
     * @param count 要清除的事件数量
     */
    fun clearEvents(count: Int) {
        repeat(count) {
            eventQueue.poll()
        }
        Logger.d(TAG, "已清除 $count 个已上报事件")
    }

    /**
     * 获取设备 ID
     *
     * @return 设备唯一标识
     */
    fun getDeviceId(): String = deviceId

    /**
     * 获取内存队列中待上报的事件数量
     */
    fun getPendingCount(): Int = eventQueue.size

    /**
     * 序列化参数为 JSON 字符串
     *
     * @param params 事件参数
     * @return JSON 格式字符串
     */
    private fun serializeParams(params: Map<String, String>): String {
        if (params.isEmpty()) return "{}"
        return params.entries.joinToString(",", prefix = "{", postfix = "}") { (key, value) ->
            "\"$key\":\"${value.replace("\"", "\\\"")}\""
        }
    }

    /**
     * 获取或创建设备 ID
     */
    private fun getOrCreateDeviceId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)

        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            Logger.d(TAG, "生成新的设备 ID: $id")
        }

        return id
    }

    /**
     * 格式化参数为可读字符串
     */
    private fun formatParams(params: Map<String, String>): String {
        return if (params.isEmpty()) {
            "(无参数)"
        } else {
            params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        }
    }

    /**
     * 事件数据类
     *
     * @property event 事件名称
     * @property timestamp 事件发生时间戳（毫秒）
     * @property deviceId 设备唯一标识
     * @property params 事件参数
     */
    data class AnalyticsEvent(
        val event: String,
        val timestamp: Long,
        val deviceId: String,
        val params: Map<String, String>
    ) {
        /**
         * 转换为 JSON 格式字符串
         */
        fun toJson(): String {
            val paramsJson = params.entries.joinToString(",") { (key, value) ->
                "\"$key\":\"$value\""
            }
            return """{"event":"$event","timestamp":$timestamp,"device_id":"$deviceId","params":{$paramsJson}}"""
        }
    }
}
