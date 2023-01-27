package me.simpleHook.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import me.simpleHook.R
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.util.*
import java.io.File

object ConfigHelper {

    @Synchronized
    @SuppressLint("CommitPrefEdits")
    fun saveConfig(context: Context, packageName: String, fileName: String, config: String) {
        if (FlavorUtils.isLiteVersion) {
            if (fileName == Constant.APP_CONFIG_NAME) {
                val pref = getHookConfigPref(context)
                pref?.edit()?.putString(packageName, config)?.commit()
                    ?: context.getString(R.string.xsp_not_save_config).toast(context)
            } else {
                val pref = getHookConfigPref(context, name = Constant.EXTENSION_CONFIG_PREF)
                pref?.edit()?.putString(packageName, config)?.commit()
                    ?: context.getString(R.string.xsp_not_save_config).toast(context)
            }
        } else {
            try {
                if (!AppUtils.isAppInstalled(context, packageName) || config.isEmpty()) return
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    if (Shell.isAppGrantedRoot() == true) {
                        val suFile =
                            SuFile.open("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName/config/$fileName")
                        if (!suFile.exists()) {
                            suFile.parentFile?.mkdirs()
                            suFile.createNewFile()
                        }
                        SuFileOutputStream.open(suFile).writer().use {
                            it.write(config)
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            "failed: no root permission".toast(context)
                        }
                    }
                } else {
                    val filePath =
                        Constant.ANDROID_DATA_PATH + packageName + Constant.CONFIG_DIRECTORY
                    FileUtils.writeTextToFile(config, filePath, fileName)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    "这应该是出现了bug".toast(context)
                }
            }
        }
    }

    fun deleteConfig(context: Context, packageName: String, fileName: String) {
        if (FlavorUtils.isLiteVersion) {
            if (fileName == Constant.APP_CONFIG_NAME) {
                val pref = getHookConfigPref(context)
                pref?.edit()?.remove(packageName)?.commit()
                    ?: context.getString(R.string.xsp_not_delete_config_succeed).toast(context)
            } else {
                val pref = getHookConfigPref(context, name = Constant.EXTENSION_CONFIG_PREF)
                pref?.edit()?.remove(packageName)?.commit()
                    ?: context.getString(R.string.xsp_not_delete_config_succeed).toast(context)
            }
        } else {
            val configPath =
                Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.APP_CONFIG_NAME
            val extensionPath =
                Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.EXTENSION_CONFIG_NAME
            if (FlavorUtils.isNormal()) {
                if (fileName == Constant.APP_CONFIG_NAME) {
                    if (FileUtils.fileIsExists(extensionPath)) {
                        FileUtils.deleteFile(configPath)
                    } else {
                        FileUtils.deleteFile(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/")
                    }
                } else {
                    if (FileUtils.fileIsExists(configPath)) {
                        FileUtils.deleteFile(extensionPath)
                    } else {
                        FileUtils.deleteFile(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/")
                    }
                }
            } else {
                if (fileName == Constant.APP_CONFIG_NAME) {
                    val exSuFile =
                        SuFile.open(Constant.ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.EXTENSION_CONFIG_PREF}")
                    if (exSuFile.exists()) {
                        val customSuFile =
                            SuFile.open(Constant.ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.APP_CONFIG_NAME}")
                        if (customSuFile.exists()) customSuFile.delete()
                    } else {
                        val appConfigSuFile =
                            SuFile.open("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
                        if (appConfigSuFile.exists()) Shell.cmd("rm -rf ${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
                            .exec()
                    }
                } else {
                    val configSuFile =
                        SuFile.open(Constant.ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.APP_CONFIG_NAME}")
                    if (configSuFile.exists()) {
                        val exSuFile =
                            SuFile.open(Constant.ROOT_CONFIG_MAIN_DIRECTORY + "$packageName/config/${Constant.EXTENSION_CONFIG_PREF}")
                        if (exSuFile.exists()) exSuFile.delete()
                    } else {
                        val appConfigSuFile =
                            SuFile.open("${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
                        if (appConfigSuFile.exists()) Shell.cmd("rm -rf ${Constant.ROOT_CONFIG_MAIN_DIRECTORY}$packageName")
                            .exec()
                    }
                }
            }
        }
    }

    fun getHookConfigPref(
        context: Context, name: String = Constant.CUSTOM_CONFIG_PREF
    ): SharedPreferences? {
        return try {
            context.getSharedPreferences(name, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            null
        }
    }

    @Synchronized
    fun insertRecordsFromFile(packageName: String): List<PrintLog> {
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
        val list = mutableListOf<PrintLog>()
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                val suFile = SuFile.open(filePath)
                if (!suFile.canRead()) return emptyList()
                SuFileInputStream.open(suFile).bufferedReader().useLines {
                    it.iterator().forEach { str ->
                        try {
                            val printLog = Gson().fromJson(str, PrintLog::class.java)
                            list.add(printLog)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
                suFile.delete()
            } else {
                if (!FileUtils.fileIsExists(filePath)) return emptyList()
                File(filePath).useLines {
                    it.iterator().forEach { str ->
                        try {
                            val printLog = Gson().fromJson(str, PrintLog::class.java)
                            list.add(printLog)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
                FileUtils.deleteFile(filePath)
            }
        } catch (e: Exception) {
            FileUtils.writeLogToFile(
                e.stackTraceToString(),
                filePath = "/storage/emulated/0/Android/data/me.simpleHook/files/log.txt",
                size = 512
            )
        }
        return list
    }
}