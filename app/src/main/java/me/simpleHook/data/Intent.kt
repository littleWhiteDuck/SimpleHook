package me.simpleHook.data


@kotlinx.serialization.Serializable
data class IntentBean(
    val packageName: String,
    val className: String,
    val action: String,
    val data: String,
    val extras: List<ExtraBean>
)

@kotlinx.serialization.Serializable
data class ExtraBean(val type: String, val key: String, val value: String)