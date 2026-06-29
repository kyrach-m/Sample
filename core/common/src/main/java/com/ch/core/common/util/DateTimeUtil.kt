package com.ch.core.common.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 时间相关工具类
 */
object DateTimeUtil {

    private const val MINUTE_MILLIS = 60 * 1000L
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 将时间戳（毫秒）转换为友好时间字符串
     *
     * - 1 分钟内 → "刚刚"
     * - 1~59 分钟 → "X分钟前"
     * - 1~23 小时 → "X小时前"
     * - 今天 → "今天 HH:mm"
     * - 昨天 → "昨天 HH:mm"
     * - 今年 → "MM-dd HH:mm"
     * - 更早 → "yyyy-MM-dd HH:mm"
     *
     * @param showSeconds 是否显示秒（仅"刚刚"场景）
     */
    fun Long.toFriendlyTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - this

        return when {
            // 1 分钟内
            diff < MINUTE_MILLIS -> "刚刚"

            // 1 小时内
            diff < HOUR_MILLIS -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "${minutes}分钟前"
            }

            // 24 小时内
            diff < DAY_MILLIS -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "${hours}小时前"
            }

            // 同一自然年
            else -> {
                val nowCal = Calendar.getInstance()
                val targetCal = Calendar.getInstance().apply { timeInMillis = this@toFriendlyTime }

                when {
                    // 今天
                    isSameDay(nowCal, targetCal) -> {
                        "今天 ${timeFormat.format(Date(this))}"
                    }
                    // 昨天
                    isYesterday(nowCal, targetCal) -> {
                        "昨天 ${timeFormat.format(Date(this))}"
                    }
                    // 今年
                    nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) -> {
                        "${dateFormat.format(Date(this))} ${timeFormat.format(Date(this))}"
                    }
                    // 更早
                    else -> {
                        "${fullFormat.format(Date(this))} ${timeFormat.format(Date(this))}"
                    }
                }
            }
        }
    }

    /** 判断两个 Calendar 是否为同一天 */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /** 判断 target 是否为 now 的昨天 */
    private fun isYesterday(now: Calendar, target: Calendar): Boolean {
        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, target)
    }
}
