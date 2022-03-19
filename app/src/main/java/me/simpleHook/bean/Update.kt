package me.simpleHook.bean

import androidx.annotation.Keep

@Keep
data class Update(
    val versionName: String,
    val versionCode: Int,
    val title: String,
    val message: String,
    val downloadUrl: String,
    val isForce: Boolean
)
