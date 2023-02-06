package me.simpleHook.compat

import me.simpleHook.util.AppUtils

interface ConfigSystem {

    fun isEnableDelete(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || isEnableSave(packageName)
    }

    fun isEnableSave(packageName: String): Boolean

    fun saveCustomConfig(packageName: String, content: String): Boolean

    fun deleteCustomConfig(packageName: String): Boolean

    fun saveExConfig(packageName: String, content: String): Boolean

    fun deleteExConfig(packageName: String): Boolean
}