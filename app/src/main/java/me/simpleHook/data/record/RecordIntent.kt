package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("Intent")
data class RecordIntent(
    val packageName: String,
    val className: String,
    val action: String,
    val data: String,
    val extras: List<RecordIntentExtra>
): Record()


@Serializable
@SerialName("IntentExtra")
data class RecordIntentExtra(
    val intentType: String, val key: String, val value: Map<RecordValueType, String>
)
