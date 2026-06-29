package com.ch.core.common.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 金额工具类
 *
 * 所有金额计算强制使用 [BigDecimal] 避免浮点精度丢失。
 * 内部单位：分（Long），显示单位：元（String）。
 */
object MoneyUtil {

    private val HUNDRED = BigDecimal("100")

    /**
     * 分转元
     *
     * @param fen 金额（单位：分）
     * @return 格式化后的元，保留两位小数，如 "12.50"
     *
     * 示例：
     * ```
     * fenToYuan(1250)  // "12.50"
     * fenToYuan(100)   // "1.00"
     * fenToYuan(-50)   // "-0.50"
     * ```
     */
    fun fenToYuan(fen: Long): String {
        return try {
            BigDecimal.valueOf(fen)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP)
                .toPlainString()
        } catch (e: Exception) {
            "0.00"
        }
    }

    /**
     * 元转分
     *
     * @param yuan 金额字符串（单位：元），如 "12.50"
     * @return 对应的分，解析失败返回 0
     *
     * 示例：
     * ```
     * yuanToFen("12.50")  // 1250
     * yuanToFen("1")      // 100
     * yuanToFen("abc")    // 0
     * ```
     */
    fun yuanToFen(yuan: String): Long {
        return try {
            BigDecimal(yuan)
                .multiply(HUNDRED)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 千位分隔符格式化
     *
     * @param number 数字
     * @return 带千位分隔符的字符串，如 "1,234,567.89"
     *
     * 示例：
     * ```
     * formatWithComma(1234567.89)  // "1,234,567.89"
     * formatWithComma(1000.0)      // "1,000.00"
     * ```
     */
    fun formatWithComma(number: Double): String {
        return try {
            val format = DecimalFormat("#,##0.00")
            format.roundingMode = RoundingMode.HALF_UP
            format.format(number)
        } catch (e: Exception) {
            "0.00"
        }
    }
}
