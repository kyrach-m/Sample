package com.ch.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ch.core.storage.db.entity.VersionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 版本信息 DAO
 *
 * 提供版本信息的增删改查操作。
 * 后续可根据业务需求扩展更多方法。
 */
@Dao
interface VersionDao {

    /**
     * 插入版本信息
     *
     * @param entity 版本实体
     * @return 插入行的 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VersionEntity): Long

    /**
     * 批量插入版本信息
     *
     * @param entities 版本实体列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VersionEntity>)

    /**
     * 查询所有版本信息
     *
     * @return 版本信息列表（响应式 Flow）
     */
    @Query("SELECT * FROM version_info ORDER BY createdAt DESC")
    fun getAll(): Flow<List<VersionEntity>>

    /**
     * 查询所有版本信息（一次性）
     *
     * @return 版本信息列表
     */
    @Query("SELECT * FROM version_info ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<VersionEntity>

    /**
     * 根据 ID 查询版本信息
     *
     * @param id 版本 ID
     * @return 版本信息，不存在返回 null
     */
    @Query("SELECT * FROM version_info WHERE id = :id")
    suspend fun getById(id: Long): VersionEntity?

    /**
     * 查询最新版本信息
     *
     * @return 最新版本，不存在返回 null
     */
    @Query("SELECT * FROM version_info ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): VersionEntity?

    /**
     * 删除指定 ID 的版本信息
     *
     * @param id 版本 ID
     * @return 删除的行数
     */
    @Query("DELETE FROM version_info WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * 清空所有版本信息
     */
    @Query("DELETE FROM version_info")
    suspend fun deleteAll()
}
