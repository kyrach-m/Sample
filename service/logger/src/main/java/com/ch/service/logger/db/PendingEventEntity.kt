package com.ch.service.logger.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待上报埋点事件实体
 *
 * 对应数据库表 pending_events。
 * 每条记录表示一条待上报的埋点事件，支持重试机制。
 *
 * @property id 唯一标识（自增主键）
 * @property eventName 事件名称
 * @property params 事件参数（JSON 字符串）
 * @property timestamp 事件发生时间戳（毫秒）
 * @property deviceId 设备唯一标识
 * @property retryCount 已重试次数
 */
@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 事件名称 */
    val eventName: String,

    /** 事件参数（JSON 格式字符串） */
    val params: String,

    /** 事件发生时间戳 */
    val timestamp: Long,

    /** 设备唯一标识 */
    val deviceId: String,

    /** 已重试次数（最多 3 次） */
    val retryCount: Int = 0
) {
    companion object {
        /** 最大重试次数 */
        const val MAX_RETRY_COUNT = 3
    }
}
