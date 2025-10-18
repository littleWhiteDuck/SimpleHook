package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Base64Operation {
    Encode,
    Decode,
}

@Serializable
@SerialName("Base64")
data class RecordBase64(
    val operation: Base64Operation,
    val rawData: Map<RecordValueType, String>,
    val result: Map<RecordValueType, String>,
    val stackDetail: String,
) : Record()

