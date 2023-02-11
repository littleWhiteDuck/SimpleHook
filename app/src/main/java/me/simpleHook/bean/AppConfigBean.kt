package me.simpleHook.bean

import me.simpleHook.database.entity.AppConfig

data class AppConfigBean(val appConfig: AppConfig, var drag: Boolean = false)
