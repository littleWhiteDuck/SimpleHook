package me.simpleHook.bean

data class AssistGroup(
    val title: String,
    val items: List<AssistItem>
)
data class AssistItem(
    val title: String,
    var isChecked: Boolean,
    val tag: String,
    val other: String = ""
)
