package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("Mac")
data class RecordHmac(
    val algorithm: String,
    val key: Map<RecordValueType, String>?,
    val rawData: Map<RecordValueType, String>,
    val resultData: Map<RecordValueType, String>,
    val stackDetail: String
) : Record()
