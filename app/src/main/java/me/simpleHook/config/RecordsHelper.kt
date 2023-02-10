package me.simpleHook.config

import android.content.Context
import android.provider.DocumentsContract
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.compat.DocumentCompatUtils
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
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
        val filePath =
            Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
        val list = mutableListOf<PrintLog>()
        try {
            if (FlavorUtils.rootVersion) {
                val suFile = SuFile.open(filePath)
                if (!suFile.canRead()) return emptyList()
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
            } else if (OSUtils.atLeastR()) {
//                val fileUri = Uri.parse(changeToUri())
                val fileUri = DocumentCompatUtils.generateFileUri(
                    packageName,
                    Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
                )
                if (!DocumentCompatUtils.isFileExists(context, fileUri)) return emptyList()
                context.contentResolver.openInputStream(fileUri)!!.also { inputStream ->
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    bufferedReader.useLines {
                        it.iterator().forEach { str ->
                            try {
                                list.add(Json.decodeFromString(str))
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                DocumentsContract.deleteDocument(context.contentResolver, fileUri)

            } else {
                if (!FileUtils.isFileExists(filePath)) return emptyList()
                File(filePath).useLines {
                    it.iterator().forEach { str ->
                        try {
                            val printLog = Json.decodeFromString<PrintLog>(str)
                            list.add(printLog)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
                FileUtils.deleteFile(filePath)
            }
        } catch (e: Throwable) {
            LogUtils.outLog(e.stackTraceToString())
        }
        return list
    }
}