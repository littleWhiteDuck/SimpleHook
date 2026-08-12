package me.simpleHook.data.config

import me.simpleHook.core.utils.AppUtil

interface ConfigSystem {

    fun isEnableDelete(packageName: String): Boolean {
        return !AppUtil.isAppInstalled(packageName) || isEnableSave(packageName)
    }

    fun isEnableSave(packageName: String): Boolean

    fun saveCustomConfig(packageName: String, content: String): Boolean

    fun deleteCustomConfig(packageName: String): Boolean

    fun saveExConfig(packageName: String, content: String): Boolean

    fun deleteExConfig(packageName: String): Boolean
}