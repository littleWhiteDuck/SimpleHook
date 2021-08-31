package me.simpleHook.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class AppConfig(
    @ColumnInfo(name = "packageName")
    var packageName:String,
    @ColumnInfo(name = "appName")
    var appName:String,
    @ColumnInfo(name = "versionName")
    var versionName:String,
    @ColumnInfo(name = "description")
    var description:String,
    @ColumnInfo(name = "app_config")
    var config:String,
    @ColumnInfo(name = "canUse")
    var canUse:Boolean = true,
    @PrimaryKey(autoGenerate = true)
    var id:Int = 0
) : Parcelable
