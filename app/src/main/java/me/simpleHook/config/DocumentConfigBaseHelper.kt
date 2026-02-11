package me.simpleHook.config

import me.simpleHook.App
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.constant.Constant
import me.simpleHook.utils.AppUtil

abstract class DocumentConfigBaseHelper : ConfigSystem {

    protected abstract fun hasPermission(packageName: String): Boolean

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtil.isAppInstalled(packageName) || !me.simpleHook.GlobalValue.sp.checkPermission || hasPermission(
            packageName
        )
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        return DocumentCompat.outTextToFile(
            App,
            packageName,
            Constant.CUSTOM_CONFIG_NORMAL_NAME,
            content
        )
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val filePath = Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY +
                Constant.CUSTOM_CONFIG_NORMAL_NAME
        return DocumentCompat.deleteFile(packageName, filePath)
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        return DocumentCompat.outTextToFile(
            App,
            packageName,
            Constant.EXTENSION_CONFIG_NORMAL_NAME,
            content
        )
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val filePath = Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY +
                Constant.EXTENSION_CONFIG_NORMAL_NAME
        return DocumentCompat.deleteFile(packageName, filePath)
    }
}
