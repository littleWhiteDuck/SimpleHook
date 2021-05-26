package me.simpleHook.bean

import android.graphics.drawable.Drawable

data class MainItem(
    val appName:String,
    val icon:Drawable,
    val desc:String,
    val canUse:Boolean)