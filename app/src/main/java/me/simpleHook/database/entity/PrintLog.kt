package me.simpleHook.database.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity
@kotlinx.serialization.Serializable
data class PrintLog(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "log") var log: String,
    @ColumnInfo(name = "packageName") var packageName: String,
    @ColumnInfo(name = "read") var read: Boolean = false,
    @ColumnInfo(name = "type") var type: String = "need_update",
    var isMark: Boolean = false,
    var time: String = "need_update"
) : Parcelable