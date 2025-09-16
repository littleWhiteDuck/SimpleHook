package me.simpleHook.data

import me.simpleHook.database.entity.AppConfig

data class ConfigItem(
    val appConfig: AppConfig, var isChecked: Boolean = false
)
