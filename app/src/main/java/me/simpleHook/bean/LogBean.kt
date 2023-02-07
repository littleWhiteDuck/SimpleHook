package me.simpleHook.bean

import androidx.annotation.Keep
import kotlinx.serialization.Contextual

@Keep
@kotlinx.serialization.Serializable
data class LogBean(
    val type: String, @Contextual val other: List<String>, val packageName: String
)