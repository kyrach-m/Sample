package com.ch.core.storage.db.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room 类型转换器
 *
 * 将 Room 不支持的类型转换为支持的类型进行存储。
 * 通过 @TypeConverters 注解在 AppDatabase 上全局注册。
 *
 * 支持的转换：
 * - Date ↔ Long（时间戳）
 * - List<String> ↔ String（用 "|" 分隔）
 *
 * 用法示例：
 * ```kotlin
 * @Entity
 * data class User(
 *     val name: String,
 *     val birthday: Date,          // 自动转换为 Long 存储
 *     val tags: List<String>       // 自动转换为 "tag1|tag2|tag3" 存储
 * )
 * ```
 *
 * 扩展指南：
 * 如需添加新的类型转换，在此文件中添加对应的 @TypeConverter 方法即可。
 */
class Converters {

    /**
     * Date → Long
     *
     * 将 Date 转换为时间戳（毫秒）存储到数据库。
     *
     * @param value Date 对象
     * @return 时间戳（毫秒），null 返回 null
     */
    @TypeConverter
    fun fromDate(value: Date?): Long? {
        return value?.time
    }

    /**
     * Long → Date
     *
     * 将时间戳（毫秒）转换为 Date 对象。
     *
     * @param value 时间戳（毫秒）
     * @return Date 对象，null 返回 null
     */
    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * List<String> → String
     *
     * 将字符串列表转换为 "|" 分隔的字符串存储。
     * 空列表返回空字符串。
     *
     * @param list 字符串列表
     * @return 分隔后的字符串
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(DELIMITER) ?: ""
    }

    /**
     * String → List<String>
     *
     * 将 "|" 分隔的字符串还原为字符串列表。
     * 空字符串返回空列表。
     *
     * @param value 分隔后的字符串
     * @return 字符串列表
     */
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(DELIMITER)
    }

    companion object {
        /**
         * 列表分隔符
         *
         * 使用 "|" 作为分隔符，因为该字符在常规文本中较少出现。
         */
        private const val DELIMITER = "|"
    }
}
