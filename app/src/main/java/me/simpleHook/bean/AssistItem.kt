package me.simpleHook.bean

data class AssistItem(
    val title: String,
    var isChecked: Boolean,
    val tag: String,
    val other: String = ""
)
