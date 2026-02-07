package me.simpleHook.data

import me.simpleHook.data.record.RecordType


data class RecordPart(val packageName: String, val type: RecordType, val subType: String)


sealed class RecordShowItem

data class RecordShowPack(val packageName: String, val count: Int = 0) : RecordShowItem()
data class RecordShowType(
    val type: RecordType,
    val subType: String,
    val count: Int = 0,
) : RecordShowItem()