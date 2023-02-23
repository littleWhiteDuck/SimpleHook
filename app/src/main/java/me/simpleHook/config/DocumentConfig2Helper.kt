package me.simpleHook.config

import me.simpleHook.App
import me.simpleHook.GlobalValue
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.constant.Constant
import me.simpleHook.util.AppUtils
import me.simpleHook.util.PermissionUtils

class DocumentConfig2Helper : ConfigSystem {

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || !GlobalValue.sp.checkPermission || PermissionUtils.isGrantPackage(
            packageName)
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return DocumentCompat.outTextToFile(App,
            packageName,
            Constant.CUSTOM_CONFIG_NORMAL_NAME,
            content)
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY + Constant.CUSTOM_CONFIG_NORMAL_NAME
        return DocumentCompat.deleteFile(packageName, filePath)
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return DocumentCompat.outTextToFile(App,
            packageName,
            Constant.EXTENSION_CONFIG_NORMAL_NAME,
            content)
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY + Constant.EXTENSION_CONFIG_NORMAL_NAME
        return DocumentCompat.deleteFile(packageName, filePath)
    }
}