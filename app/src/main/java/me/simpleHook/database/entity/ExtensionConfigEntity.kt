package me.simpleHook.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Entity
@Serializable
data class ExtensionConfigEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var appName: String,
    var packageName: String,
    var config: String = "",
    var enable: Boolean = true,
): Parcelable
