package com.ch.core.common.util

/**
 * 字符串相关工具类
 */
object StringUtil {

    /**
     * 中国大陆手机号正则
     *
     * 规则：1 开头，第二位 3-9，共 11 位数字
     */
    private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")

    /**
     * Email 正则（RFC 5322 简化版）
     */
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )

    /**
     * 判断字符串是否为有效的中国大陆手机号
     *
     * @return true 表示有效手机号
     */
    fun String.isValidPhone(): Boolean {
        return isNotBlank() && PHONE_REGEX.matches(this.trim())
    }

    /**
     * 判断字符串是否为有效的 Email 地址
     *
     * @return true 表示有效邮箱
     */
    fun String.isValidEmail(): Boolean {
        return isNotBlank() && EMAIL_REGEX.matches(this.trim())
    }

    /**
     * 判断字符串是否为空或全空白
     */
    fun String.isBlankOrNull(): Boolean {
        return isNullOrEmpty() || isBlank()
    }

    /**
     * 截取字符串，超过指定长度时追加省略号
     *
     * @param maxLength 最大长度
     * @param suffix 省略号后缀，默认 "..."
     */
    fun String.ellipsize(maxLength: Int, suffix: String = "..."): String {
        return if (length > maxLength) {
            substring(0, maxLength) + suffix
        } else {
            this
        }
    }
}
