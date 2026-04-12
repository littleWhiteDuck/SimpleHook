package me.simplehook.plugin.old.data

@kotlinx.serialization.Serializable
data class AppConfig(
    var packageName: String,
    var appName: String,
    var versionName: String,
    var description: String,
    var configs: String,
    var enable: Boolean = true,
    var id: Int = 0
)