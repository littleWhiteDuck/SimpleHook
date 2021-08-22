package me.simpleHook.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PrintLog(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "log")
    var log: String,
    @ColumnInfo(name = "packageName")
    var packageName:String
)