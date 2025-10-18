package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.simpleHook.R

@Serializable
@SerialName("Json")
data class RecordJson(
    val jsonType: RecordJsonType,
    val values: Map<String, String>,
    val stackDetail: String,
) : Record()

enum class RecordJsonType(val displayId: Int) {
    JsonObjectPut(displayId = R.string.record_json_object_put),
    JsonObjectCreate(displayId = R.string.record_json_object_create),
    JsonArrayPut(displayId = R.string.record_json_array_put),
    JsonArrayCreate(displayId = R.string.record_json_array_create),
}