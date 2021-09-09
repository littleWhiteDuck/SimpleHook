package me.simpleHook.bean

data class AssistConfigBean(
    val all: Boolean = false,
    val dialog: Boolean = false,
    val popup: Boolean = false,
    val diaCancel: Boolean = false,
    val toast: Boolean = false,
    val intent: Boolean = false,
    val tinker: Boolean = false,
    val vpn: Boolean = false,
    val click: Boolean = false,
    val popCancel: Boolean = false,
    val xposed: Boolean = false
)