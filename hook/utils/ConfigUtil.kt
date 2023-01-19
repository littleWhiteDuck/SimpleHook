package me.simpleHook.hook.utils

import me.simpleHook.constant.Constant
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.log
import java.io.File
import java.io.FileNotFoundException

object ConfigUtil {

    fun getConfigFromFile(
        packageName: String,
        configName: String = Constant.APP_CONFIG_NAME
    ): String? {
        val configPath = if (FlavorUtils.isNormal()) {
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/"
        } else {
            Constant.ROOT_CONFIG_MAIN_DIRECTORY + packageName + "/config/"
        } + configName
        return try {
            val strConfig = File(configPath).reader().use { it.readText() }
            strConfig
        } catch (e: FileNotFoundException) {
            "failed: $configPath".log(packageName)
            null
        }
    }
}