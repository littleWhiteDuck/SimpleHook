package me.simpleHook.bean

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
data class IntentBean(
    val packageName: String,
    val className: String,
    val action: String,
    val data: String,
    val extras: List<ExtraBean>
)

@Keep
@kotlinx.serialization.Serializable
data class ExtraBean(val type: String, val key: String, val value: String)