package me.simpleHook.data.local.configstore

import me.simpleHook.core.GlobalValue
import me.simpleHook.data.config.ConfigSystem
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.platform.shizuku.ShizukuFileManager
import me.simpleHook.core.utils.AppUtil

class ShizukuConfigHelper : ConfigSystem {
    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtil.isAppInstalled(packageName) || !GlobalValue.sp.checkPermission || ShizukuFileManager.isAvailable
    }

    override fun saveCustomConfig(
        packageName: String,
        content: String
    ): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val savePath = String.format(ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, packageName)
        return ShizukuFileManager.service?.writeFile(savePath, content) ?: false
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        val path = String.format(ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, packageName)
        return ShizukuFileManager.service?.deleteFile(path) ?: false
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val savePath = String.format(ConfigConstant.ROOT_EXTENSION_CONFIG_PATH, packageName)
        return ShizukuFileManager.service?.writeFile(savePath, content) ?: false
    }

    override fun deleteExConfig(packageName: String): Boolean {
        val path = String.format(ConfigConstant.ROOT_EXTENSION_CONFIG_PATH, packageName)
        return ShizukuFileManager.service?.deleteFile(path) ?: false
    }
}
