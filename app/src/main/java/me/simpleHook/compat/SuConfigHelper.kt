package me.simpleHook.compat

import android.util.Log
import com.topjohnwu.superuser.Shell
import me.simpleHook.constant.Constant
import me.simpleHook.util.AppUtils

class SuConfigHelper : ConfigSystem {
    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || Shell.isAppGrantedRoot() == true
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val path =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.CUSTOM_CONFIG_NAME}"
        return SuFileUtils.outTextToFile(path, content)
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val path =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.CUSTOM_CONFIG_NAME}"
        return SuFileUtils.deleteFile(path)
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val path =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.EXTENSION_CONFIG_NAME}"
        return SuFileUtils.outTextToFile(path, content)
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val path =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.EXTENSION_CONFIG_NAME}"
        return SuFileUtils.deleteFile(path)
    }

}