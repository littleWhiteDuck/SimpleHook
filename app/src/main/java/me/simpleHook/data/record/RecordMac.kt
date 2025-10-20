package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Hmac")
data class RecordMac(
    val algorithm: String,
    val rawData: Map<RecordValueType, String>,
    val resultData: Map<RecordValueType, String>,
    val stackDetail: String
): Record()
