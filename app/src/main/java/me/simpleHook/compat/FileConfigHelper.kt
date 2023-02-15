package me.simpleHook.compat

import me.simpleHook.App
import me.simpleHook.GlobalServices
import me.simpleHook.constant.Constant.ANDROID_DATA_PATH
import me.simpleHook.constant.Constant.CONFIG_DIRECTORY
import me.simpleHook.constant.Constant.CUSTOM_CONFIG_NORMAL_NAME
import me.simpleHook.constant.Constant.EXTENSION_CONFIG_NORMAL_NAME
import me.simpleHook.util.AppUtils
import me.simpleHook.util.FileUtils
import me.simpleHook.util.PermissionUtils

class FileConfigHelper : ConfigSystem {

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || !GlobalServices.sp.checkPermission || PermissionUtils.isGrantWritePermission(
            App
        )
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + CUSTOM_CONFIG_NORMAL_NAME
        return FileUtils.outTextToFile(filePath, content)
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + CUSTOM_CONFIG_NORMAL_NAME
        return FileUtils.deleteFile(filePath)
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + EXTENSION_CONFIG_NORMAL_NAME
        return FileUtils.outTextToFile(filePath, content)
    }

    override fun deleteExConfig(packageName: String): Boolean {
        val filePath =
            ANDROID_DATA_PATH + packageName + CONFIG_DIRECTORY + EXTENSION_CONFIG_NORMAL_NAME
        return FileUtils.deleteFile(filePath)
    }

}