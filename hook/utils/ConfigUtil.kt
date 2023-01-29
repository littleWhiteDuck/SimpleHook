package me.simpleHook.hook.utils

import android.annotation.SuppressLint
import android.net.Uri
import com.google.gson.Gson
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.utils.HookHelper.appContext
import me.simpleHook.hook.utils.HookHelper.hostPackageName
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.log
import java.io.File
import java.io.FileNotFoundException

object ConfigUtil {
    private val uri = Uri.parse("content://me.simplehook.provider/app_configs")
    private val assistUri = Uri.parse("content://me.simplehook.provider/assist_configs")

    fun getConfigFromFile(
        configName: String = Constant.APP_CONFIG_NAME
    ): String? {
        val configPath = if (FlavorUtils.isNormal()) {
            Constant.ANDROID_DATA_PATH + hostPackageName + "/simpleHook/config/"
        } else {
            Constant.ROOT_CONFIG_MAIN_DIRECTORY + hostPackageName + "/config/"
        } + configName
        return try {
            val strConfig = File(configPath).reader().use { it.readText() }
            strConfig
        } catch (e: FileNotFoundException) {
            "failed: $configPath".log(hostPackageName)
            null
        }
    }

    @SuppressLint("Range")
    fun getCustomConfigFromDB(): String? {
        return try {
            var config: String? = null
            appContext.contentResolver?.query(
                uri, null, "packageName = ?", arrayOf(hostPackageName), null
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
                        config = Gson().toJson(appConfig)
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
            appContext.contentResolver?.query(
                assistUri, null, "packageName = ?", arrayOf(hostPackageName), null
            )?.apply {
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