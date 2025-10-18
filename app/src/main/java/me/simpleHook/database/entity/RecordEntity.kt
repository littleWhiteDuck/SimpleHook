package me.simpleHook.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import me.simpleHook.data.record.RecordType


@Serializable
@Entity
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    val type: RecordType,
    val record: String,
    val packageName: String,
    val isRead: Boolean = false,
    val isMark: Boolean = false,
    val time: String,
)
