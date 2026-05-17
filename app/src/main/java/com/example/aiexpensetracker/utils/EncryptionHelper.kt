package com.example.aiexpensetracker.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object EncryptionHelper {
    // 🔴 这是一个硬编码的密钥。
    // 在商业级应用中，你应该让用户输入密码生成密钥，或者使用 Android KeyStore。
    // 但对于个人项目，这样做已经能防止普通人偷看你的数据了。
    private const val SECRET_KEY = "MySuperSecretKey_ChangeThis123"

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    // 生成 32 字节的 Key (AES-256)
    private fun getKey(): SecretKeySpec {
        val sha = MessageDigest.getInstance("SHA-256")
        val key = sha.digest(SECRET_KEY.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(key, "AES")
    }

    // 生成 16 字节的 IV (为了简单，这里用固定 IV，商业级应用应使用随机 IV 并拼接到密文前)
    private fun getIv(): IvParameterSpec {
        val iv = ByteArray(16) // 默认全是 0
        return IvParameterSpec(iv)
    }

    // 🔒 加密： String (JSON) -> String (Base64乱码)
    fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), getIv())
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    // 🔓 解密： String (Base64乱码) -> String (JSON)
    fun decrypt(encryptedData: String): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), getIv())
            val decodedBytes = Base64.decode(encryptedData, Base64.NO_WRAP)
            val originalBytes = cipher.doFinal(decodedBytes)
            return String(originalBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "" // 解密失败 (可能密钥不对，或文件损坏)
        }
    }
}