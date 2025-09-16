package me.simpleHook.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
@kotlinx.serialization.Serializable
data class CollectionEntity(
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "config")
    var config: String,
    @ColumnInfo(name = "type")
    var type: String = "custom",
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
)