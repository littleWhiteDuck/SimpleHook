package me.simpleHook.config

import me.simpleHook.App
import me.simpleHook.GlobalValue
import me.simpleHook.constant.Constant.ANDROID_DATA_PATH
import me.simpleHook.constant.Constant.CONFIG_DIRECTORY
import me.simpleHook.constant.Constant.CUSTOM_CONFIG_NORMAL_NAME
import me.simpleHook.constant.Constant.EXTENSION_CONFIG_NORMAL_NAME
import me.simpleHook.utils.AppUtil
import me.simpleHook.utils.FileUtil
import me.simpleHook.utils.PermissionUtil

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