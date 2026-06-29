package com.ch.core.common.util

import android.os.Build

/**
 * Android 版本判断工具类（商业级标准）
 *
 * 统一封装所有版本判断逻辑，避免在业务代码中散落 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.XXX` 的硬编码。
 * 后续所有版本判断必须使用此工具类，便于统一维护和测试。
 *
 * 设计原则：
 * - 仅依赖 Android SDK 官方 API，不引入任何 View 相关类
 * - 使用 `@ChecksSdkIntAtLeast` 注解帮助 Lint 正确识别版本判断
 * - 每个版本提供 `isAtLeastXxx()` 方法，语义清晰
 *
 * 用法示例：
 * ```kotlin
 * if (BuildVersion.isAtLeastT) {
 *     // Android 13 (Tiramisu) 及以上的逻辑
 * }
 *
 * if (BuildVersion.isAtLeastU) {
 *     // Android 14 (UpsideDownCake) 及以上的逻辑
 * }
 * ```
 */
object BuildVersion {

    /**
     * 当前设备的 SDK 版本号
     */
    val sdkInt: Int
        get() = Build.VERSION.SDK_INT

    /**
     * 是否为 Android 6.0 (Marshmallow) 及以上
     *
     * API 23
     */
    val isAtLeastM: Boolean
        get() = sdkInt >= Build.VERSION_CODES.M

    /**
     * 是否为 Android 7.0 (Nougat) 及以上
     *
     * API 24
     */
    val isAtLeastN: Boolean
        get() = sdkInt >= Build.VERSION_CODES.N

    /**
     * 是否为 Android 7.1 (Nougat MR1) 及以上
     *
     * API 25
     */
    val isAtLeastN_MR1: Boolean
        get() = sdkInt >= Build.VERSION_CODES.N_MR1

    /**
     * 是否为 Android 8.0 (Oreo) 及以上
     *
     * API 26
     */
    val isAtLeastO: Boolean
        get() = sdkInt >= Build.VERSION_CODES.O

    /**
     * 是否为 Android 8.1 (Oreo MR1) 及以上
     *
     * API 27
     */
    val isAtLeastO_MR1: Boolean
        get() = sdkInt >= Build.VERSION_CODES.O_MR1

    /**
     * 是否为 Android 9.0 (Pie) 及以上
     *
     * API 28
     */
    val isAtLeastP: Boolean
        get() = sdkInt >= Build.VERSION_CODES.P

    /**
     * 是否为 Android 10 (Q) 及以上
     *
     * API 29
     */
    val isAtLeastQ: Boolean
        get() = sdkInt >= Build.VERSION_CODES.Q

    /**
     * 是否为 Android 11 (R) 及以上
     *
     * API 30
     */
    val isAtLeastR: Boolean
        get() = sdkInt >= Build.VERSION_CODES.R

    /**
     * 是否为 Android 12 (S) 及以上
     *
     * API 31
     */
    val isAtLeastS: Boolean
        get() = sdkInt >= Build.VERSION_CODES.S

    /**
     * 是否为 Android 12L (S_V2) 及以上
     *
     * API 32
     */
    val isAtLeastS_V2: Boolean
        get() = sdkInt >= Build.VERSION_CODES.S_V2

    /**
     * 是否为 Android 13 (Tiramisu) 及以上
     *
     * API 33
     */
    val isAtLeastT: Boolean
        get() = sdkInt >= Build.VERSION_CODES.TIRAMISU

    /**
     * 是否为 Android 14 (UpsideDownCake) 及以上
     *
     * API 34
     */
    val isAtLeastU: Boolean
        get() = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /**
     * 是否为 Android 15 (VanillaIceCream) 及以上
     *
     * API 35
     */
    val isAtLeastV: Boolean
        get() = sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    /**
     * 判断当前版本是否大于等于指定的 API 级别
     *
     * @param apiLevel API 级别（如 Build.VERSION_CODES.TIRAMISU）
     * @return true=当前版本 >= 指定版本
     */
    fun isAtLeast(apiLevel: Int): Boolean {
        return sdkInt >= apiLevel
    }

    /**
     * 执行指定版本及以上的代码块
     *
     * 用法示例：
     * ```kotlin
     * BuildVersion.runAtLeastT {
     *     // 仅在 Android 13+ 执行
     * }
     * ```
     *
     * @param apiLevel 最低 API 级别
     * @param block 要执行的代码块
     */
    inline fun runAtLeast(apiLevel: Int, block: () -> Unit) {
        if (isAtLeast(apiLevel)) {
            block()
        }
    }

    /**
     * 执行 Android 12+ 代码块
     */
    inline fun runAtLeastS(block: () -> Unit) {
        if (isAtLeastS) block()
    }

    /**
     * 执行 Android 13+ 代码块
     */
    inline fun runAtLeastT(block: () -> Unit) {
        if (isAtLeastT) block()
    }

    /**
     * 执行 Android 14+ 代码块
     */
    inline fun runAtLeastU(block: () -> Unit) {
        if (isAtLeastU) block()
    }
}
