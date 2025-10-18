package me.simpleHook.hook.utils

import me.simpleHook.constant.ConfigConstant
import me.simpleHook.hook.utils.HookHelper.hostPackageName
import me.simpleHook.utils.FlavorUtil
import java.io.File

object ConfigUtil {
    fun getCustomConfigFromFile(): String? {
        val configPath = if (FlavorUtil.rootVersion) {
            String.format(format = ConfigConstant.ROOT_CUSTOM_CONFIG_PATH, hostPackageName)
        } else {
            String.format(format = ConfigConstant.NORMAL_CUSTOM_CONFIG_PATH, hostPackageName)
        }
        return getConfigFromFile(configPath)
    }

    fun getExtConfigFromFile(): String? {
        val configPath = if (FlavorUtil.rootVersion) {
            String.format(format = ConfigConstant.ROOT_EXTENSION_CONFIG_PATH, hostPackageName)
        } else {
            String.format(format = ConfigConstant.NORMAL_EXTENSION_CONFIG_PATH, hostPackageName)
        }
        return getConfigFromFile(configPath)
    }

    private fun getConfigFromFile(configPath: String): String? = runCatching {
        File(configPath).readText()
    }.onFailure {
        "failed: $configPath".xLog()
    }.getOrNull()
}