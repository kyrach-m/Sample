package com.ch.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.ch.core.storage.db.converter.Converters
import com.ch.core.storage.db.cipher.SqlCipherKeyManager
import com.ch.core.storage.db.dao.VersionDao
import com.ch.core.storage.db.entity.VersionEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Room 数据库（商业级增强版）
 *
 * 功能特性：
 * - SQLCipher 加密：数据库文件加密，防止 root 设备读取
 * - 版本迁移：支持从版本 1 到 2 的平滑迁移
 * - 类型转换：Date ↔ Long，List<String> ↔ String
 * - Schema 导出：便于版本追踪和迁移验证
 *
 * 用法示例：
 * ```kotlin
 * // 获取加密数据库实例
 * val database = AppDatabase.getInstance(context)
 *
 * // 获取 DAO
 * val versionDao = database.versionDao()
 *
 * // 插入数据
 * versionDao.insert(VersionEntity(version = "1.0.0", buildNumber = 1))
 *
 * // 查询数据（Flow 响应式）
 * versionDao.getAll().collect { versions ->
 *     // 处理版本列表
 * }
 * ```
 *
 * 数据库升级流程：
 * 1. 修改 version 值（如 2 -> 3）
 * 2. 添加新的 Entity 和 DAO
 * 3. 在 Migrations.kt 中添加迁移策略
 * 4. 将新迁移加入 ALL_MIGRATIONS 数组
 *
 * ⚠️ 安全说明：
 * - 数据库使用 SQLCipher 加密（AES-256）
 * - 密钥通过 Android Keystore 保护，禁止硬编码
 * - 详见 SqlCipherKeyManager
 */
@Database(
    entities = [VersionEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 获取版本信息 DAO
     *
     * @return VersionDao 实例
     */
    abstract fun versionDao(): VersionDao

    companion object {
        /**
         * 数据库文件名
         */
        private const val DATABASE_NAME = "app_database.db"

        /**
         * 单例实例
         */
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * 获取数据库单例
         *
         * 使用双重检查锁保证线程安全。
         * 首次创建时使用 SQLCipher 加密。
         *
         * @param context Application Context
         * @return AppDatabase 实例
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        /**
         * 预热数据库
         *
         * 在启动时调用以提前初始化数据库实例，避免首次访问时卡顿。
         * 仅触发实例创建和连接建立，不执行查询。
         *
         * @param context Application Context
         */
        fun warmUp(context: Context) {
            getInstance(context)
        }

        /**
         * 重置数据库单例
         *
         * 用于测试或需要重新创建数据库的场景。
         * 调用后需重新调用 getInstance() 获取新实例。
         */
        fun resetInstance() {
            synchronized(this) {
                instance = null
            }
        }

        /**
         * 构建加密数据库
         *
         * 使用 SQLCipher 加密，密钥通过 Android Keystore 管理。
         * 注册所有迁移策略，不使用 fallbackToDestructiveMigration。
         *
         * @param context Application Context
         * @return AppDatabase 实例
         */
        private fun buildDatabase(context: Context): AppDatabase {
            // 获取 SQLCipher 加密密钥（通过 Android Keystore）
            val passphrase = SqlCipherKeyManager.getDatabaseKey()
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // 注册所有数据库迁移
                .addMigrations(*ALL_MIGRATIONS)
                // 使用 SQLCipher 加密工厂
                .openHelperFactory(factory)
                .build()
        }
    }
}
