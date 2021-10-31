package me.simpleHook.util

import android.text.TextUtils
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CipherUtils {

    private const val CipherMode = "AES/CFB/NoPadding"
    private const val KEY = "simpleHook___@@@____simpleHook<>"

    private fun generateKey(): SecretKeySpec {
        val data: ByteArray = KEY.toByteArray(StandardCharsets.UTF_8)
        return SecretKeySpec(data, "AES")
    }

    /**
     * Encrypt a string
     *
     * @param data Source string
     * @return Encrypted string
     */
    fun encrypt(data: String, start: String = "config://"): String? {
        return if (TextUtils.isEmpty(data)) {
            null
        } else try {
            val cipher = Cipher.getInstance(CipherMode)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                generateKey(),
                IvParameterSpec(ByteArray(cipher.blockSize))
            )
            val encrypted = cipher.doFinal(data.toByteArray())
            start + Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decrypted a string
     *
     * @param data Encrypted string
     * @return Decrypted string
     */
    fun decrypt(data: String): String? {
        return try {
            val encrypted = Base64.decode(data.toByteArray(), Base64.DEFAULT)
            val cipher = Cipher.getInstance(CipherMode)
            cipher.init(
                Cipher.DECRYPT_MODE,
                generateKey(),
                IvParameterSpec(ByteArray(cipher.blockSize))
            )
            val original = cipher.doFinal(encrypted)
            String(original, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
