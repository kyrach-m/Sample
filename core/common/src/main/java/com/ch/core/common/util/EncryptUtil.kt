package com.ch.core.common.util

import android.util.Base64
import java.security.MessageDigest

/**
 * 加密工具类
 *
 * 提供 MD5、SHA-256、Base64 等常用加密/编码方法。
 * 所有方法内部使用 try-catch 包裹，异常时返回空字符串，绝不抛异常。
 */
object EncryptUtil {

    /**
     * 计算字符串的 MD5 摘要
     *
     * @param input 待加密字符串
     * @return 32 位小写 MD5 哈希值，异常时返回空字符串
     */
    fun md5(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 计算字符串的 SHA-256 摘要
     *
     * @param input 待加密字符串
     * @return 64 位小写 SHA-256 哈希值，异常时返回空字符串
     */
    fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Base64 编码
     *
     * @param byteArray 待编码的字节数组
     * @return Base64 编码后的字符串，异常时返回空字符串
     */
    fun base64Encode(byteArray: ByteArray): String {
        return try {
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Base64 解码
     *
     * @param input Base64 编码的字符串
     * @return 解码后的字节数组，异常时返回空数组
     */
    fun base64Decode(input: String): ByteArray {
        return try {
            Base64.decode(input, Base64.NO_WRAP)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}
