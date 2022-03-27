package me.simpleHook.bean

import androidx.room.ColumnInfo


data class RecordBean(
    @ColumnInfo(name = "packageName") var packageName: String,
    @ColumnInfo(name = "type") var type: String = "need_update",
)
