package me.simpleHook.data

import me.simpleHook.data.record.RecordType


data class RecordPackageCount(val packageName: String, val count: Int)
data class RecordTypeCount(val type: String, val count: Int)


sealed class RecordShowItem

data class RecordShowPack(val packageName: String, val count: Int = 0) : RecordShowItem()
data class RecordShowType(
    val type: RecordType,
    val subType: String,
    val count: Int = 0,
) : RecordShowItem()
