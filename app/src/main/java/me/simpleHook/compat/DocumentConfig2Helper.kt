package me.simpleHook.compat

import me.simpleHook.SimpleHookApp
import me.simpleHook.constant.Constant
import me.simpleHook.util.AppUtils
import me.simpleHook.util.PermissionUtils

class DocumentConfig2Helper : ConfigSystem {

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || PermissionUtils.isGrantPackage(packageName)
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return DocumentCompatUtils.outTextToFile(
            SimpleHookApp.app, packageName, Constant.CUSTOM_CONFIG_NORMAL_NAME, content
        )
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY + Constant.CUSTOM_CONFIG_NORMAL_NAME
        return DocumentCompatUtils.deleteFile(filePath)
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return DocumentCompatUtils.outTextToFile(
            SimpleHookApp.app, packageName, Constant.EXTENSION_CONFIG_NORMAL_NAME, content
        )
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY + Constant.EXTENSION_CONFIG_NORMAL_NAME
        return DocumentCompatUtils.deleteFile(filePath)
    }
}