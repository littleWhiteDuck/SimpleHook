package me.simpleHook.hook.util

import android.annotation.SuppressLint
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.extension.log
import me.simpleHook.hook.util.HookHelper.appContext
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.FlavorUtils.PROVIDER_CUSTOM_CONFIG_URI
import me.simpleHook.util.FlavorUtils.PROVIDER_EXTENSION_CONFIG_URI
import java.io.File

object ConfigUtil {
    private val uri = Uri.parse(PROVIDER_CUSTOM_CONFIG_URI)
    private val extensionUri = Uri.parse(PROVIDER_EXTENSION_CONFIG_URI)

    fun getConfigFromFile(
        configName: String = Constant.CUSTOM_CONFIG_NAME
    ): String? {
        val configPath = if (FlavorUtils.rootVersion) {
            Constant.ROOT_CONFIG_MAIN_DIRECTORY + hostPackageName + "/config/"
        } else {
            Constant.ANDROID_DATA_PATH + hostPackageName + "/simpleHook/config/"
        } + configName
        return runCatching {
            File(configPath).readText()
        }.onFailure {
            "failed: $configPath".log(hostPackageName)
        }.getOrNull()
    }

    @SuppressLint("Range")
    fun getCustomConfigFromDB(): String? {
        return try {
            var config: String? = null
            appContext.contentResolver?.query(uri,
                null,
                "packageName = ?",
                arrayOf(hostPackageName),
                null)?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("enable")) == 1) {
                        val configString = getString(getColumnIndex("config"))
                        val appConfig = AppConfig(configs = configString,
                            packageName = hostPackageName,
                            appName = "",
                            versionName = "",
                            description = "")
                        config = Json.encodeToString(appConfig)
                        break
                    }
                }
                close()
            }
            config
        } catch (e: Throwable) {
            null
        }
    }

    @SuppressLint("Range")
    fun getExConfigFromDB(): String? {
        return try {
            var config: String? = null
            appContext.contentResolver?.query(extensionUri,
                null,
                "packageName = ?",
                arrayOf(hostPackageName),
                null)?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("allSwitch")) == 1) {
                        config = getString(getColumnIndex("config"))
                    }
                }
                close()
            }
            config
        } catch (e: Throwable) {
            null
        }
    }
}