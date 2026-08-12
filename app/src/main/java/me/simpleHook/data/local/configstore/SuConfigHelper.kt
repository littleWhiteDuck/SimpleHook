package me.simpleHook.data.local.configstore

import me.simpleHook.core.GlobalValue
import me.simpleHook.data.config.ConfigSystem
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.SuFileUtil
import me.simpleHook.core.utils.SuUtil

class SuConfigHelper : ConfigSystem {
    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtil.isAppInstalled(packageName) || !GlobalValue.sp.checkPermission || SuUtil.isGrantedRoot()
    }

    @Synchronized
    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val path = String.format(ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, packageName)
        return SuFileUtil.outTextToFile(path, content)
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val customPath = String.format(ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, packageName)
        val extensionPath = String.format(ConfigConstant.ROOT_EXTENSION_CONFIG_PATH, packageName)
        return if (SuFileUtil.isFileExists(extensionPath)) {
            SuFileUtil.deleteFile(customPath)
        } else {
            SuUtil.deleteFile(getSimpleHookRootPath(packageName))
        }
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val path = String.format(ConfigConstant.ROOT_EXTENSION_CONFIG_PATH, packageName)
        return SuFileUtil.outTextToFile(path, content)
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtil.isAppInstalled(packageName)) return true
        val customPath = String.format(ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, packageName)
        val extensionPath = String.format(ConfigConstant.ROOT_EXTENSION_CONFIG_PATH, packageName)
        return if (SuFileUtil.isFileExists(customPath)) {
            SuFileUtil.deleteFile(extensionPath)
        } else {
            SuUtil.deleteFile(getSimpleHookRootPath(packageName))
        }
    }

    private fun getSimpleHookRootPath(packageName: String): String {
        val customPath = String.format(ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, packageName)
        return customPath.substringBeforeLast("/config")
    }

}
