package me.simpleHook.bean

data class Update(
    val versionName: String,
    val versionCode: Int,
    val title: String,
    val message: String,
    val downloadUrl: String,
    val isForce: Boolean
)
