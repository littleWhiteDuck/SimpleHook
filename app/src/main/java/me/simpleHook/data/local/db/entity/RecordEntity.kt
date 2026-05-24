package me.simpleHook.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import me.simpleHook.data.record.RecordType


@Serializable
@Entity(
    indices = [
        Index(value = ["packageName", "time"]),
        Index(value = ["type", "time"]),
        Index(value = ["isMark", "packageName", "time"]),
        Index(value = ["isMark", "type", "time"]),
        Index(value = ["sourceKey"], unique = true)
    ]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val type: RecordType,
    val subType: String,
    val record: String,
    val packageName: String,
    val processName: String = packageName,
    val isRead: Boolean = false,
    val isMark: Boolean = false,
    val time: String,
    val sourceKey: String? = null,
)
