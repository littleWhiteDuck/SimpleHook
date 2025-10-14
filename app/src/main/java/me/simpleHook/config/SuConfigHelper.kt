package me.simpleHook.config

import me.simpleHook.GlobalValue
import me.simpleHook.constant.ConfigConstant
import me.simpleHook.constant.Constant
import me.simpleHook.utils.AppUtil
import me.simpleHook.utils.FileUtil
import me.simpleHook.utils.SuFileUtil
import me.simpleHook.utils.SuUtil

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
        return if (FileUtil.isFileExists(extensionPath)) {
            SuFileUtil.deleteFile(customPath)
        } else {
            SuUtil.deleteFile("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
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
        return if (FileUtil.isFileExists(customPath)) {
            SuFileUtil.deleteFile(extensionPath)
        } else {
            SuUtil.deleteFile("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
        }
    }

}