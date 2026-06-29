package com.ch.core.storage.db.cipher

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ch.core.storage.kv.KVStorage
import com.ch.core.storage.kv.Scope
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * SQLCipher 数据库密钥管理器
 *
 * 使用 Android Keystore 保护 SQLCipher 数据库密钥。
 * 所有密钥在运行时生成，禁止硬编码。
 *
 * 安全架构：
 * ```
 * Android Keystore（硬件级保护）
 *   └── AES-256 主密钥（永不离开 Keystore）
 *         └── 加密 SQLCipher 数据库密钥
 *               └── 加密后的密文存储在 MMKV
 * ```
 *
 * ⚠️ 安全警告：
 * - 禁止硬编码任何密钥
 * - 禁止将明文密钥存储到 MMKV 或 SharedPreferences
 * - 所有密钥必须通过 Android Keystore 生成和管理
 * - 正式环境必须启用设备锁屏保护
 *
 * 用法示例：
 * ```kotlin
 * // 获取数据库密钥（首次调用自动生成）
 * val passphrase = SqlCipherKeyManager.getDatabaseKey()
 *
 * // 重置数据库密钥（用户退出登录时调用）
 * SqlCipherKeyManager.resetDatabaseKey()
 * ```
 */
object SqlCipherKeyManager {

    /**
     * Android Keystore 提供者名称
     */
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * 密钥别名
     */
    private const val KEY_ALIAS = "app_db_master_key"

    /**
     * AES 加密算法
     */
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"

    /**
     * MMKV 存储键：加密后的数据库密钥
     */
    private const val MMKV_KEY_ENCRYPTED_PASSPHRASE = "db_encrypted_passphrase"

    /**
     * MMKV 存储键：IV 向量
     */
    private const val MMKV_KEY_IV = "db_passphrase_iv"

    /**
     * SQLCipher 密钥长度（字节）
     *
     * SQLCipher 使用 256 位密钥 = 32 字节
     */
    private const val PASSPHRASE_LENGTH = 32

    /**
     * 获取数据库密钥
     *
     * 首次调用时：
     * 1. 在 Android Keystore 中生成 AES-256 主密钥
     * 2. 生成随机 SQLCipher 数据库密钥
     * 3. 用主密钥加密数据库密钥
     * 4. 将加密后的密钥和 IV 存入 MMKV
     *
     * 后续调用时：
     * 1. 从 MMKV 读取加密密钥和 IV
     * 2. 从 Keystore 获取主密钥
     * 3. 解密并返回明文数据库密钥
     *
     * @return SQLCipher 数据库密钥（ByteArray）
     */
    fun getDatabaseKey(): ByteArray {
        // 检查是否已有加密密钥
        val encryptedPassphrase = KVStorage.getString(MMKV_KEY_ENCRYPTED_PASSPHRASE, scope = Scope.CONFIG)
        val ivString = KVStorage.getString(MMKV_KEY_IV, scope = Scope.CONFIG)

        if (encryptedPassphrase.isNotEmpty() && ivString.isNotEmpty()) {
            // 已有密钥，解密返回
            return decryptPassphrase(encryptedPassphrase, ivString)
        }

        // 首次调用，生成新密钥
        return generateAndStoreNewPassphrase()
    }

    /**
     * 重置数据库密钥
     *
     * 清除 MMKV 中存储的加密密钥。
     * 调用后需要重新创建数据库（旧数据将不可恢复）。
     *
     * 注意：Android Keystore 中的主密钥不会被删除，
     * 下次调用 getDatabaseKey() 时会用同一个主密钥加密新密钥。
     */
    fun resetDatabaseKey() {
        KVStorage.remove(MMKV_KEY_ENCRYPTED_PASSPHRASE, Scope.CONFIG)
        KVStorage.remove(MMKV_KEY_IV, Scope.CONFIG)
    }

    /**
     * 生成新的数据库密钥并加密存储
     *
     * @return 明文数据库密钥
     */
    private fun generateAndStoreNewPassphrase(): ByteArray {
        // 1. 确保 Keystore 中存在主密钥
        val masterKey = getOrCreateMasterKey()

        // 2. 生成随机 SQLCipher 密钥
        val passphrase = ByteArray(PASSPHRASE_LENGTH).also { bytes ->
            java.security.SecureRandom().nextBytes(bytes)
        }

        // 3. 用主密钥加密
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encryptedBytes = cipher.doFinal(passphrase)
        val iv = cipher.iv

        // 4. 存入 MMKV（Base64 编码）
        val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
        KVStorage.putString(MMKV_KEY_ENCRYPTED_PASSPHRASE, encryptedString, Scope.CONFIG)
        KVStorage.putString(MMKV_KEY_IV, ivString, Scope.CONFIG)

        return passphrase
    }

    /**
     * 解密数据库密钥
     *
     * @param encryptedPassphrase Base64 编码的加密密钥
     * @param ivString Base64 编码的 IV
     * @return 明文数据库密钥
     */
    private fun decryptPassphrase(encryptedPassphrase: String, ivString: String): ByteArray {
        val masterKey = getOrCreateMasterKey()
        val encryptedBytes = Base64.decode(encryptedPassphrase, Base64.NO_WRAP)
        val iv = Base64.decode(ivString, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, IvParameterSpec(iv))
        return cipher.doFinal(encryptedBytes)
    }

    /**
     * 获取或创建 Keystore 主密钥
     *
     * 如果 Keystore 中已存在别名对应的密钥，直接返回。
     * 否则生成新的 AES-256 密钥并存入 Keystore。
     *
     * @return AES-256 主密钥
     */
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // 检查是否已有密钥
        val existingKey = keyStore.getEntry(KEY_ALIAS, null)
        if (existingKey is KeyStore.SecretKeyEntry) {
            return existingKey.secretKey
        }

        // 生成新的 AES-256 密钥
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setKeySize(256)
            // 要求设备有锁屏保护（可选，增强安全性）
            // .setUserAuthenticationRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
