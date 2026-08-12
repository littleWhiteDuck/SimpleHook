package me.simpleHook.data.record


data class RecordDetailItem(
    val title: String,
    val content: String,
    val fullContent: String = content,
    val isTruncated: Boolean = false,
)
