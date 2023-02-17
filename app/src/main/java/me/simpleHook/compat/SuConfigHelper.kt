package me.simpleHook.compat

import me.simpleHook.GlobalServices
import me.simpleHook.constant.Constant
import me.simpleHook.util.AppUtils
import me.simpleHook.util.FileUtils
import me.simpleHook.util.SuUtil

class SuConfigHelper : ConfigSystem {
    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || !GlobalServices.sp.checkPermission || SuUtil.isGrantedRoot()
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
        val customPath =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.CUSTOM_CONFIG_NAME}"
        val extensionPath =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.EXTENSION_CONFIG_NAME}"
        return if (FileUtils.isFileExists(extensionPath)) {
            SuFileUtils.deleteFile(customPath)
        } else {
            SuUtil.deleteFile("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
        }
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val path =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.EXTENSION_CONFIG_NAME}"
        return SuFileUtils.outTextToFile(path, content)
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        val customPath =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.CUSTOM_CONFIG_NAME}"
        val extensionPath =
            "${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/${Constant.EXTENSION_CONFIG_NAME}"
        return if (FileUtils.isFileExists(customPath)) {
            SuFileUtils.deleteFile(extensionPath)
        } else {
            SuUtil.deleteFile("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
        }
    }

}