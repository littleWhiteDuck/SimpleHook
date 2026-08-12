package me.simpleHook.platform.hook.utils

import io.github.qauxv.loader.sbl.lsp101.Lsp101HookImpl
import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.platform.hook.utils.HookHelper.hostPackageName
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader

object HookConfigFileUtil {
    fun getCustomConfigFromFile(): String? {
        val configPath = String.format(
            format = ConfigConstant.ROOT_CUSTOM_CONFIG_PATH,
            hostPackageName
        )
        return getConfigFromFile(
            configPath,
            ConfigConstant.customRemoteConfigFileName(hostPackageName)
        )
    }

    fun getExtensionConfigFromFile(): String? {
        val configPath = String.format(
            format = ConfigConstant.ROOT_EXTENSION_CONFIG_PATH,
            hostPackageName
        )
        return getConfigFromFile(
            configPath,
            ConfigConstant.extensionRemoteConfigFileName(hostPackageName)
        )
    }

    private fun getConfigFromFile(configPath: String, remoteFileName: String): String? {
        val localContent = runCatching {
            File(configPath).readText()
        }.onFailure {
            "failed: $configPath".xLog()
        }.getOrNull()

        if (!localContent.isNullOrEmpty()) {
            return localContent
        }

        return getConfigFromRemoteFile(remoteFileName)
    }

    private fun getConfigFromRemoteFile(remoteFileName: String): String? {
        val lspModule = Lsp101HookImpl.self ?: return null
        return runCatching {
            lspModule.openRemoteFile(remoteFileName).use { descriptor ->
                FileReader(descriptor.fileDescriptor).use { reader ->
                    reader.readText()
                }
            }
        }.onFailure {
            when (it) {
                is FileNotFoundException -> "remote file not found: $remoteFileName".xLog()
                else -> "failed remote: $remoteFileName".xLog()
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}
