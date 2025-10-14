package me.simpleHook.hook.utils

import android.annotation.SuppressLint
import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import me.simpleHook.constant.ConfigConstant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.utils.HookHelper.appContext
import me.simpleHook.hook.utils.HookHelper.hostPackageName
import me.simpleHook.utils.FlavorUtil
import me.simpleHook.utils.FlavorUtil.PROVIDER_CUSTOM_CONFIG_URI
import me.simpleHook.utils.FlavorUtil.PROVIDER_EXTENSION_CONFIG_URI
import java.io.File

object ConfigUtil {
    private val uri = PROVIDER_CUSTOM_CONFIG_URI.toUri()
    private val extensionUri = PROVIDER_EXTENSION_CONFIG_URI.toUri()

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
        "failed: $configPath".log()
    }.getOrNull()

    @SuppressLint("Range")
    fun getCustomConfigFromDB(): String? {
        return try {
            var config: String? = null
            appContext.contentResolver?.query(
                uri,
                null,
                "packageName = ?",
                arrayOf(hostPackageName),
                null
            )?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("enable")) == 1) {
                        val configString = getString(getColumnIndex("config"))
                        val appConfig = AppConfig(
                            configs = configString,
                            packageName = hostPackageName,
                            appName = "",
                            versionName = "",
                            description = ""
                        )
                        config = Json.encodeToString(appConfig)
                        break
                    }
                }
                close()
            }
            config
        } catch (_: Throwable) {
            null
        }
    }

    @SuppressLint("Range")
    fun getExConfigFromDB(): String? {
        return try {
            var config: String? = null
            appContext.contentResolver?.query(
                extensionUri,
                null,
                "packageName = ?",
                arrayOf(hostPackageName),
                null
            )?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("allSwitch")) == 1) {
                        config = getString(getColumnIndex("config"))
                    }
                }
                close()
            }
            config
        } catch (_: Throwable) {
            null
        }
    }
}