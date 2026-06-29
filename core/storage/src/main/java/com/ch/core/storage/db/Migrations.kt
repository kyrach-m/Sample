package com.ch.core.storage.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库迁移集合
 *
 * 所有数据库版本迁移策略集中管理。
 * 每个 Migration 对象对应一个版本升级路径。
 *
 * 注意：
 * - 迁移必须是幂等的（多次执行结果一致）
 * - 新增表/列时必须提供默认值
 * - 正式环境禁止使用 fallbackToDestructiveMigration
 *
 * 用法示例：
 * ```kotlin
 * // 在 AppDatabase.builder 中注册
 * Room.databaseBuilder(context, AppDatabase::class.java, "app_database.db")
 *     .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
 *     .build()
 * ```
 *
 * 迁移编写规范：
 * 1. ALTER TABLE 添加列时必须指定 DEFAULT 值
 * 2. CREATE TABLE IF NOT EXISTS 防止重复创建
 * 3. 数据迁移操作应在事务中完成
 */

/**
 * 版本 1 → 2 迁移
 *
 * 变更内容：
 * - 新增 users 表（示例迁移）
 * - 包含 id、name、avatar_url、created_at 字段
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL DEFAULT '',
                `avatar_url` TEXT NOT NULL DEFAULT '',
                `created_at` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

/**
 * 所有迁移列表（按版本顺序排列）
 *
 * 新增迁移时，在此列表末尾追加即可。
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2
)
