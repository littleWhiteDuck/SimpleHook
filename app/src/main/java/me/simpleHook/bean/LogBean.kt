package me.simpleHook.bean

import androidx.annotation.Keep

@Keep
data class LogBean(
    val type: String,
    val other: List<Any>,
    val packageName: String
)

@Keep
data class LogBean2(
    val type: String, val other: List<IntentBean>, val packageName: String
)