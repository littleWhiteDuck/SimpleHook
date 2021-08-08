package me.simpleHook.bean

import android.graphics.drawable.Drawable

data class AppItem(
    val name:String,
    val icon:Drawable,
    val packageName:String,
    val versionName:String,
    val installedTime:String
)