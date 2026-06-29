package com.ch.core.common.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer

/**
 * JSON 工具类，基于 kotlinx.serialization
 *
 * 提供安全的编解码方法，解析失败返回 null 而不抛异常
 */
object JsonUtil {

    /**
     * 全局 Json 配置
     *
     * - ignoreUnknownKeys: 忽略未知字段，防止服务端新增字段导致解析失败
     * - isLenient: 宽松模式，允许非标准 JSON
     * - encodeDefaults: 编码默认值
     */
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * 安全解码 JSON 字符串为指定类型
     *
     * 解析失败时返回 null，不抛出异常
     *
     * 用法示例：
     * ```
     * val user: User? = JsonUtil.safeDecode(jsonString)
     * val list: List<Item>? = JsonUtil.safeDecode(jsonString)
     * ```
     *
     * @param T 目标类型，必须使用 @Serializable 注解
     * @param jsonString 待解析的 JSON 字符串
     * @return 解析成功返回对象，失败返回 null
     */
    inline fun <reified T> safeDecode(jsonString: String): T? {
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 安全编码对象为 JSON 字符串
     *
     * 编码失败时返回 null，不抛出异常
     *
     * @param T 源类型，必须使用 @Serializable 注解
     * @param data 待编码的对象
     * @return 编码成功返回 JSON 字符串，失败返回 null
     */
    inline fun <reified T> safeEncode(data: T): String? {
        return try {
            json.encodeToString(data)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 直接解码（失败会抛异常）
     *
     * 仅在确定 JSON 格式正确时使用
     */
    inline fun <reified T> decode(jsonString: String): T {
        return json.decodeFromString(jsonString)
    }

    /**
     * 直接编码（失败会抛异常）
     */
    inline fun <reified T> encode(data: T): String {
        return json.encodeToString(data)
    }
}
