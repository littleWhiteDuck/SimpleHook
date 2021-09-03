package me.simpleHook.bean

import androidx.annotation.Keep

@Keep
data class LogBean(val type: String, val other: List<Any>)