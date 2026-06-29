package com.ch.service.logger.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 待上报埋点事件 DAO
 *
 * 提供对 pending_events 表的增删查操作。
 *
 * 操作说明：
 * - [insertEvent]：插入新事件（冲突时替换）
 * - [getPendingEvents]：获取待上报事件（按时间升序，限制数量）
 * - [deleteEvents]：删除指定 ID 的事件（上报成功后调用）
 * - [incrementRetryCount]：增加重试次数
 * - [getExceededRetryEvents]：获取超过最大重试次数的事件
 * - [deleteExceededEvents]：删除超过最大重试次数的事件
 * - [getPendingCount]：获取待上报事件总数
 * - [deleteAll]：清空所有事件
 */
@Dao
interface PendingEventDao {

    /**
     * 插入一条事件
     *
     * @param event 事件实体
     * @return 插入的行 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PendingEventEntity): Long

    /**
     * 批量插入事件
     *
     * @param events 事件列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<PendingEventEntity>)

    /**
     * 获取待上报事件
     *
     * 按时间升序返回，确保先上报最早的事件。
     *
     * @param limit 最大返回数量
     * @return 待上报事件列表
     */
    @Query("SELECT * FROM pending_events ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int): List<PendingEventEntity>

    /**
     * 删除指定 ID 的事件
     *
     * 上报成功后调用，移除已成功上报的事件。
     *
     * @param ids 事件 ID 列表
     */
    @Query("DELETE FROM pending_events WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<Long>)

    /**
     * 增加指定事件的重试次数
     *
     * @param id 事件 ID
     */
    @Query("UPDATE pending_events SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    /**
     * 获取超过最大重试次数的事件
     *
     * @return 超过重试限制的事件列表
     */
    @Query("SELECT * FROM pending_events WHERE retryCount >= :maxRetry")
    suspend fun getExceededRetryEvents(maxRetry: Int): List<PendingEventEntity>

    /**
     * 删除超过最大重试次数的事件
     */
    @Query("DELETE FROM pending_events WHERE retryCount >= :maxRetry")
    suspend fun deleteExceededEvents(maxRetry: Int)

    /**
     * 获取待上报事件总数
     *
     * @return 事件数量
     */
    @Query("SELECT COUNT(*) FROM pending_events")
    suspend fun getPendingCount(): Int

    /**
     * 清空所有事件
     */
    @Query("DELETE FROM pending_events")
    suspend fun deleteAll()
}
