package com.ch.service.logger.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 埋点事件本地缓存数据库
 *
 * 使用 Room 实现，存储待上报的埋点事件。
 * 支持离线缓存、断网重传、重试机制。
 *
 * 数据库版本：1
 * 表：pending_events（待上报事件）
 *
 * 使用方式：
 * ```kotlin
 * val database = AnalyticsDatabase.getInstance(context)
 * val dao = database.pendingEventDao()
 * ```
 */
@Database(
    entities = [PendingEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AnalyticsDatabase : RoomDatabase() {

    /**
     * 获取待上报事件 DAO
     */
    abstract fun pendingEventDao(): PendingEventDao

    companion object {
        private const val DATABASE_NAME = "analytics.db"

        @Volatile
        private var instance: AnalyticsDatabase? = null

        /**
         * 获取数据库单例
         *
         * @param context Application Context
         * @return 数据库实例
         */
        fun getInstance(context: Context): AnalyticsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
