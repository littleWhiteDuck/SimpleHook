package me.simpleHook.config

import android.content.Context
import android.provider.DocumentsContract
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.serialization.json.Json
import me.simpleHook.App
import me.simpleHook.GlobalValue
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.constant.ConfigConstant
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.shizuku.ShizukuFileManager
import me.simpleHook.util.FileUtils
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.LogUtils
import me.simpleHook.util.OSUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RecordsHelper {

    @Synchronized
    fun insertRecordsFromFile(context: Context, packageName: String): List<PrintLog> {
        val recordPath = String.format(format = ConfigConstant.RECORD_LOG_TEMP_PATH, packageName)
        val list = mutableListOf<PrintLog>()
        return runCatching {
            if (FlavorUtils.rootVersion) {
                if (GlobalValue.isRootWork) {
                    if (rootReadRecords(recordPath, list)) return emptyList()
                } else {
                    if (shizukuReadRecords(recordPath, list)) return emptyList()
                }
            } else {
                if (fileReadRecords(packageName, recordPath, context, list)) return emptyList()
            }
            list
        }.onFailure {
            LogUtils.outLog(it.stackTraceToString())
        }.getOrDefault(emptyList())
    }


    private fun shizukuReadRecords(
        filePath: String,
        list: MutableList<PrintLog>
    ): Boolean {
        if (!ShizukuFileManager.isAvailable) return true
        val file = App.externalCacheDir!!.resolve("logs")
        ShizukuFileManager.service?.copyFile(filePath, file.path)
        ShizukuFileManager.service?.deleteFile(filePath)
        if (!file.canRead()) return true
        file.bufferedReader().useLines {
            it.iterator().forEach { str ->
                try {
                    val printLog = Json.decodeFromString<PrintLog>(str)
                    list.add(printLog)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        file.delete()
        return false
    }

    private fun rootReadRecords(
        filePath: String,
        list: MutableList<PrintLog>
    ): Boolean {
        val suFile = SuFile.open(filePath)
        if (!suFile.canRead()) return true
        SuFileInputStream.open(suFile).bufferedReader().useLines {
            it.iterator().forEach { str ->
                try {
                    val printLog = Json.decodeFromString<PrintLog>(str)
                    list.add(printLog)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        suFile.delete()
        return false
    }


    private fun fileReadRecords(
        packageName: String,
        recordPath: String,
        context: Context,
        list: MutableList<PrintLog>
    ): Boolean {
        if (OSUtils.atLeastR()) {
            val fileUri = DocumentCompat.generateFileUri(packageName, recordPath)
            if (!DocumentCompat.isFileExists(context, fileUri)) return true
            context.contentResolver.openInputStream(fileUri)!!.also { inputStream ->
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                bufferedReader.useLines {
                    it.iterator().forEach { str ->
                        try {
                            list.add(Json.decodeFromString(str))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            DocumentsContract.deleteDocument(context.contentResolver, fileUri)
            return false
        } else {
            if (!FileUtils.isFileExists(recordPath)) return true
            File(recordPath).useLines {
                it.iterator().forEach { str ->
                    try {
                        val printLog = Json.decodeFromString<PrintLog>(str)
                        list.add(printLog)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
            FileUtils.deleteFile(recordPath)
            return false
        }
    }
}