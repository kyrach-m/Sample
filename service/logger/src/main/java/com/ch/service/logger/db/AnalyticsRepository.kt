package com.ch.service.logger.db

import android.content.Context
import com.ch.core.common.logger.Logger

/**
 * 埋点事件仓库
 *
 * 封装 Room DAO 操作，提供埋点事件的本地存储能力。
 *
 * 核心功能：
 * - [save]：保存事件到本地数据库
 * - [getPendingEvents]：获取待上报事件
 * - [deleteEvents]：删除已成功上报的事件
 * - [incrementRetryCount]：增加重试次数
 * - [cleanExceededEvents]：清理超过重试上限的事件
 *
 * 使用示例：
 * ```kotlin
 * val repository = AnalyticsRepository.getInstance(context)
 * repository.save(PendingEventEntity(...))
 * val pending = repository.getPendingEvents(50)
 * ```
 */
class AnalyticsRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "AnalyticsRepository"

        @Volatile
        private var instance: AnalyticsRepository? = null

        /**
         * 获取仓库单例
         *
         * @param context Application Context
         * @return 仓库实例
         */
        fun getInstance(context: Context): AnalyticsRepository {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Room DAO
     */
    private val dao: PendingEventDao =
        AnalyticsDatabase.getInstance(context).pendingEventDao()

    /**
     * 保存事件到本地数据库
     *
     * @param event 待上报事件
     * @return 插入的行 ID
     */
    suspend fun save(event: PendingEventEntity): Long {
        return try {
            dao.insertEvent(event)
        } catch (e: Exception) {
            Logger.e(TAG, "保存事件失败: ${event.eventName}", e)
            -1L
        }
    }

    /**
     * 获取待上报事件
     *
     * @param limit 最大返回数量，默认 50
     * @return 待上报事件列表
     */
    suspend fun getPendingEvents(limit: Int = 50): List<PendingEventEntity> {
        return try {
            dao.getPendingEvents(limit)
        } catch (e: Exception) {
            Logger.e(TAG, "获取待上报事件失败", e)
            emptyList()
        }
    }

    /**
     * 删除已成功上报的事件
     *
     * @param ids 事件 ID 列表
     */
    suspend fun deleteEvents(ids: List<Long>) {
        try {
            dao.deleteEvents(ids)
        } catch (e: Exception) {
            Logger.e(TAG, "删除事件失败", e)
        }
    }

    /**
     * 增加重试次数
     *
     * @param id 事件 ID
     */
    suspend fun incrementRetryCount(id: Long) {
        try {
            dao.incrementRetryCount(id)
        } catch (e: Exception) {
            Logger.e(TAG, "增加重试次数失败", e)
        }
    }

    /**
     * 清理超过重试上限的事件
     *
     * 超过 [PendingEventEntity.MAX_RETRY_COUNT] 次重试的事件将被删除。
     */
    suspend fun cleanExceededEvents() {
        try {
            dao.deleteExceededEvents(PendingEventEntity.MAX_RETRY_COUNT)
        } catch (e: Exception) {
            Logger.e(TAG, "清理超限事件失败", e)
        }
    }

    /**
     * 获取待上报事件总数
     */
    suspend fun getPendingCount(): Int {
        return try {
            dao.getPendingCount()
        } catch (e: Exception) {
            Logger.e(TAG, "获取待上报数量失败", e)
            0
        }
    }
}
