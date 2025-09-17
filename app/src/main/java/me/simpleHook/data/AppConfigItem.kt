package me.simpleHook.data

import me.simpleHook.database.entity.AppConfig

data class AppConfigItem(val appConfig: AppConfig, var drag: Boolean = false)
