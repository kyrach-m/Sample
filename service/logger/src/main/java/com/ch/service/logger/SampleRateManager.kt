package com.ch.service.logger

import com.ch.core.common.logger.Logger
import kotlin.random.Random

/**
 * 埋点采样率管理器
 *
 * 控制不同类型事件的上报采样率，减少服务端压力。
 * 支持默认规则和远程配置动态调整。
 *
 * 核心特性：
 * - 按事件名称匹配采样率规则
 * - 默认规则：page_view 10%，click 100%，其他 50%
 * - 支持远程配置覆盖（通过 [updateSampleRates]）
 * - 采样算法：Random.nextFloat() < sampleRate 时允许上报
 * - 线程安全
 *
 * 使用示例：
 * ```kotlin
 * // 初始化
 * SampleRateManager.init()
 *
 * // 判断是否允许上报
 * if (SampleRateManager.shouldLog("page_view")) {
 *     AnalyticsHelper.logEvent("page_view", params)
 * }
 *
 * // 远程配置覆盖
 * SampleRateManager.updateSampleRates(mapOf(
 *     "page_view" to 0.05f,  // 降低到 5%
 *     "purchase" to 1.0f     // 购买事件 100% 上报
 * ))
 * ```
 *
 * 采样率说明：
 * - 1.0 = 100% 上报
 * - 0.5 = 50% 上报
 * - 0.1 = 10% 上报
 * - 0.0 = 不上报
 */
object SampleRateManager {

    private const val TAG = "SampleRateManager"

    /**
     * 默认采样率规则
     *
     * Key: 事件名称前缀或完整名称
     * Value: 采样率（0.0 ~ 1.0）
     */
    private val defaultRates = mapOf(
        "page_view" to 0.1f,
        "page_leave" to 0.1f,
        "click" to 1.0f,
        "purchase" to 1.0f,
        "error" to 1.0f
    )

    /**
     * 当前生效的采样率配置
     *
     * 优先使用远程配置，未配置的事件使用默认规则。
     */
    @Volatile
    private var currentRates: Map<String, Float> = defaultRates

    /**
     * 默认采样率（未匹配到规则时使用）
     */
    private const val DEFAULT_SAMPLE_RATE = 0.5f

    /**
     * 是否已初始化
     */
    @Volatile
    private var isInitialized = false

    /**
     * 初始化采样率管理器
     */
    fun init() {
        if (isInitialized) return
        isInitialized = true
        Logger.d(TAG, "SampleRateManager 初始化完成，默认规则: $currentRates")
    }

    /**
     * 判断事件是否应该上报
     *
     * 根据事件名称匹配采样率规则，通过随机采样决定是否上报。
     *
     * @param eventName 事件名称
     * @return true=允许上报，false=不上报
     */
    fun shouldLog(eventName: String): Boolean {
        if (!isInitialized) {
            init()
        }

        val rate = getSampleRate(eventName)

        // 100% 采样率直接通过（避免 Random 调用）
        if (rate >= 1.0f) return true

        // 0% 采样率直接拒绝
        if (rate <= 0.0f) return false

        // 随机采样
        val shouldLog = Random.nextFloat() < rate
        if (!shouldLog) {
            Logger.d(TAG, "事件 $eventName 被采样过滤 (采样率: $rate)")
        }
        return shouldLog
    }

    /**
     * 获取指定事件的采样率
     *
     * 匹配规则：
     * 1. 精确匹配事件名称
     * 2. 前缀匹配（如 "page_" 匹配所有 page_ 开头的事件）
     * 3. 使用默认采样率
     *
     * @param eventName 事件名称
     * @return 采样率（0.0 ~ 1.0）
     */
    fun getSampleRate(eventName: String): Float {
        // 精确匹配
        currentRates[eventName]?.let { return it }

        // 前缀匹配
        for ((pattern, rate) in currentRates) {
            if (eventName.startsWith(pattern)) {
                return rate
            }
        }

        // 默认采样率
        return DEFAULT_SAMPLE_RATE
    }

    /**
     * 更新采样率配置（远程配置覆盖）
     *
     * 从远程配置中心拉取后调用此方法更新配置。
     * 更新后的配置会覆盖默认规则。
     *
     * @param rates 新的采样率配置（事件名 → 采样率）
     */
    fun updateSampleRates(rates: Map<String, Float>) {
        // 合并：远程配置覆盖默认配置
        currentRates = defaultRates.toMutableMap().apply {
            putAll(rates)
        }
        Logger.d(TAG, "采样率配置已更新: $currentRates")
    }

    /**
     * 重置为默认配置
     *
     * 用于测试或清除远程配置。
     */
    fun reset() {
        currentRates = defaultRates
        Logger.d(TAG, "采样率配置已重置为默认")
    }

    /**
     * 获取当前所有采样率配置
     *
     * @return 采样率配置快照
     */
    fun getCurrentRates(): Map<String, Float> = currentRates.toMap()
}
