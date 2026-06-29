package com.ch.core.storage.kv

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * KV 存储作用域枚举
 *
 * 不同作用域使用独立的 MMKV 实例，实现数据隔离，避免 key 冲突。
 *
 * - [DEFAULT]：默认作用域，存储通用数据
 * - [USER]：用户数据（Token、个人信息），登录时写入，退出时清空
 * - [CONFIG]：App 配置（开关、偏好设置）
 * - [CACHE]：临时缓存数据，可随时清空
 */
enum class Scope(val mmkvId: String?) {
    DEFAULT(null),
    USER("user"),
    CONFIG("config"),
    CACHE("cache")
}

/**
 * 统一 KV 存储门面（基于 MMKV 多实例）
 *
 * 替代原 PreferenceUtil / MMKVHelper / MMKVManager，统一入口避免重复。
 * 所有 get 方法必须提供默认值，禁止返回 null。
 *
 * 使用前必须在 Application 中调用 [init] 初始化：
 * ```kotlin
 * class SampleApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         KVStorage.init(this)
 *     }
 * }
 * ```
 *
 * 使用示例：
 * ```kotlin
 * // 存储用户 Token（用户作用域，退出登录时可整体清空）
 * KVStorage.putString("access_token", token, Scope.USER)
 * val token = KVStorage.getString("access_token", scope = Scope.USER)
 *
 * // 存储 App 配置（配置作用域）
 * KVStorage.putBoolean("dark_mode", true, Scope.CONFIG)
 * val darkMode = KVStorage.getBoolean("dark_mode", scope = Scope.CONFIG)
 *
 * // 退出登录时清空用户数据
 * KVStorage.clear(Scope.USER)
 * ```
 *
 * 设计原则：
 * - 统一入口，避免多个 KV 工具类并存
 * - 作用域隔离，用户/配置/缓存数据互不干扰
 * - 默认值必选，所有 get 方法均返回非 null
 */
object KVStorage {

    @Volatile
    private var isInitialized = false

    private val mmkvInstances = mutableMapOf<Scope, MMKV>()

    /**
     * 初始化 MMKV 全局环境
     *
     * 必须在 Application.onCreate() 中调用，否则后续操作无效。
     *
     * @param context Application Context
     */
    fun init(context: Context) {
        if (isInitialized) return
        MMKV.initialize(context.applicationContext)
        isInitialized = true
    }

    /**
     * 获取指定作用域的 MMKV 实例
     *
     * @param scope 作用域
     * @return MMKV 实例
     * @throws IllegalStateException 如果未初始化
     */
    private fun getMMKV(scope: Scope): MMKV {
        check(isInitialized) {
            "KVStorage not initialized. Call KVStorage.init(context) in Application.onCreate()"
        }
        return mmkvInstances.getOrPut(scope) {
            if (scope.mmkvId == null) {
                MMKV.defaultMMKV()
            } else {
                MMKV.mmkvWithID(scope.mmkvId)
            }
        }
    }

    // ==================== String ====================

    fun putString(key: String, value: String, scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).encode(key, value)
    }

    fun getString(key: String, defaultValue: String = "", scope: Scope = Scope.DEFAULT): String {
        return getMMKV(scope).decodeString(key, defaultValue) ?: defaultValue
    }

    // ==================== Int ====================

    fun putInt(key: String, value: Int, scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).encode(key, value)
    }

    fun getInt(key: String, defaultValue: Int = 0, scope: Scope = Scope.DEFAULT): Int {
        return getMMKV(scope).decodeInt(key, defaultValue)
    }

    // ==================== Long ====================

    fun putLong(key: String, value: Long, scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).encode(key, value)
    }

    fun getLong(key: String, defaultValue: Long = 0L, scope: Scope = Scope.DEFAULT): Long {
        return getMMKV(scope).decodeLong(key, defaultValue)
    }

    // ==================== Float ====================

    fun putFloat(key: String, value: Float, scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).encode(key, value)
    }

    fun getFloat(key: String, defaultValue: Float = 0f, scope: Scope = Scope.DEFAULT): Float {
        return getMMKV(scope).decodeFloat(key, defaultValue)
    }

    // ==================== Boolean ====================

    fun putBoolean(key: String, value: Boolean, scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).encode(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false, scope: Scope = Scope.DEFAULT): Boolean {
        return getMMKV(scope).decodeBool(key, defaultValue)
    }

    // ==================== 删除 / 清空 ====================

    fun remove(key: String, scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).removeValueForKey(key)
    }

    fun clear(scope: Scope = Scope.DEFAULT) {
        getMMKV(scope).clearAll()
    }

    fun clearAll() {
        Scope.values().forEach { clear(it) }
    }

    fun contains(key: String, scope: Scope = Scope.DEFAULT): Boolean {
        return getMMKV(scope).containsKey(key)
    }
}
