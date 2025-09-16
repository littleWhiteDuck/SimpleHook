package me.simpleHook.data

import kotlinx.serialization.Contextual

@kotlinx.serialization.Serializable
data class LogBean(
    val type: String, @Contextual val other: List<String>, val packageName: String
)