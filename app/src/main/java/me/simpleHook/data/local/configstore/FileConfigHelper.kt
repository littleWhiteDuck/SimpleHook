package me.simpleHook.data.local.configstore

import me.simpleHook.core.App
import me.simpleHook.core.GlobalValue
import me.simpleHook.data.config.ConfigSystem
import me.simpleHook.core.constant.Constant.ANDROID_DATA_PATH
import me.simpleHook.core.constant.Constant.CONFIG_DIRECTORY
import me.simpleHook.core.constant.Constant.CUSTOM_CONFIG_NORMAL_NAME
import me.simpleHook.core.constant.Constant.EXTENSION_CONFIG_NORMAL_NAME
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.FileUtil
import me.simpleHook.core.utils.PermissionUtil

class FileConfigHelper : ConfigSystem {

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtil.isAppInstalled(packageName) || !GlobalValue.sp.checkPermission || PermissionUtil.isGrantWritePermission(
            App)
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + CUSTOM_CONFIG_NORMAL_NAME
        return FileUtil.outTextToFile(filePath, content)
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + CUSTOM_CONFIG_NORMAL_NAME
        return FileUtil.deleteFile(filePath)
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + EXTENSION_CONFIG_NORMAL_NAME
        return FileUtil.outTextToFile(filePath, content)
    }

    override fun deleteExConfig(packageName: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + EXTENSION_CONFIG_NORMAL_NAME
        return FileUtil.deleteFile(filePath)
    }

}
