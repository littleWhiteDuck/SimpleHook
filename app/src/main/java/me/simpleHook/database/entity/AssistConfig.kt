package me.simpleHook.database.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Keep
@Parcelize
@Entity
data class AssistConfig(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var config: String = "",
    var allSwitch: Boolean = false,
    var appName: String,
    var packageName: String
) : Parcelable