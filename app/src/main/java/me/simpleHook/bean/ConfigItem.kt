package me.simpleHook.bean

import me.simpleHook.database.entity.AppConfig

data class ConfigItem(
    val appConfig: AppConfig, var isChecked: Boolean = false
)
