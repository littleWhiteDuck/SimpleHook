package me.simpleHook.bean

import androidx.annotation.Keep

@Keep
data class AssistConfigBean(
    val all: Boolean = false,
    val dialog: Boolean = false,
    val popup: Boolean = false,
    val diaCancel: Boolean = false,
    val toast: Boolean = false,
    val intent: Boolean = false,
    val hotFix: Boolean = false,
    val vpn: Boolean = false,
    val click: Boolean = false,
    val popCancel: Boolean = false,
    val digest: Boolean = false,
    val hmac: Boolean = false,
    val crypt: Boolean = false,
    val base64: Boolean = false
)