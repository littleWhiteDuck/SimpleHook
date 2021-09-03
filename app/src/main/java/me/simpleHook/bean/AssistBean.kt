package me.simpleHook.bean

data class AssistTitle(val title: String)
data class AssistItem(
    val title: String,
    var isChecked: Boolean,
    val tag: String,
    val desc: String = "",
    val other: String = ""
)
