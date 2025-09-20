package me.simpleHook.data


data class Record(val packageName: String, val type: String = "need_update")

data class RecordShowPack(val packageName: String, val count: Int = 0)

data class RecordShowType(val type: String, val count: Int = 0)