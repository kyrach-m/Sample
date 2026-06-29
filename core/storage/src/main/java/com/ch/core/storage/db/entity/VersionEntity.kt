package com.ch.core.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 版本信息实体（占位）
 *
 * 用于初始化 Room 数据库。
 * 后续可替换为实际业务实体。
 *
 * 用法示例：
 * ```kotlin
 * val database = AppDatabase.getInstance(context)
 * val versionDao = database.versionDao()
 *
 * // 插入版本信息
 * versionDao.insert(VersionEntity(version = "1.0.0", buildNumber = 1))
 *
 * // 查询版本信息
 * val version = versionDao.getVersion()
 * ```
 */
@Entity(tableName = "version_info")
data class VersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val version: String,
    val buildNumber: Int,
    val createdAt: Long = System.currentTimeMillis()
)
