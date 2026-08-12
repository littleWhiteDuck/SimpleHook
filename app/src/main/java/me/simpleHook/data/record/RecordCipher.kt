package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.simpleHook.R


enum class RecordCipherMode(val displayId: Int) {
    Encrypt(displayId = R.string.record_cipher_encrypt),
    Decrypt(displayId = R.string.record_cipher_decrypt),
    Wrap(displayId = R.string.record_cipher_wrap),
    Unwrap(displayId = R.string.record_cipher_unwrap),
    Unknown(displayId = R.string.record_cipher_unknown),
}

@Serializable
@SerialName("Cipher")
data class RecordCipher(
    val algorithm: String,
    val cryptType: RecordCipherMode,
    val key: Map<RecordValueType, String>?,
    val iv: Map<RecordValueType, String>?,
    val aad: Map<RecordValueType, String>? = null,
    val rawData: Map<RecordValueType, String>,
    val resultData: Map<RecordValueType, String>,
    val stackDetail: String
) : Record()
