package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Web")
data class RecordWebLoadUrl(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) : Record()
