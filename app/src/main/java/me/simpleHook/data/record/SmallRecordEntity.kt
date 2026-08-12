package me.simpleHook.data.record

data class SmallRecordEntity(
    val id: Int = 0,
    val type: RecordType,
    val subType: String,
    val packageName: String,
    val isRead: Boolean = false,
    val isMark: Boolean = false,
    val time: String,
    val summaryTitle: String? = null,
    val summarySubtitle: String? = null,
    val summaryMeta: String? = null,
)
