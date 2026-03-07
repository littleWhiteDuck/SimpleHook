package me.simpleHook.platform.hook.utils

import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.platform.hook.utils.HookHelper.hostPackageName
import java.io.File

object HookConfigFileUtil {
    fun getCustomConfigFromFile(): String? {
        val configPath = String.format(
            format = ConfigConstant.ROOT_CUSTOM_CONFIG_PATH,
            hostPackageName
        )
        return getConfigFromFile(configPath)
    }

    fun getExtensionConfigFromFile(): String? {
        val configPath = String.format(
            format = ConfigConstant.ROOT_EXTENSION_CONFIG_PATH,
            hostPackageName
        )
        return getConfigFromFile(configPath)
    }

    private fun getConfigFromFile(configPath: String): String? = runCatching {
        File(configPath).readText()
    }.onFailure {
        "failed: $configPath".xLog()
    }.getOrNull()
}
