package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Toast")
data class RecordToast(
    val textList: List<String>,
    val stackDetail: String,
) : Record()
