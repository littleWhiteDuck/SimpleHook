package me.simpleHook.hook

import android.content.Context
import android.net.Uri
import androidx.core.content.contentValuesOf
import com.google.gson.Gson
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.util.FileUtils
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.TimeUtil
import me.simpleHook.util.tip

object LogHook {
    private val PRINT_URI = Uri.parse("content://littleWhiteDuck/print_logs")
    fun toLogMsg(context: Context?, log: String, packageName: String, type: String) {
        val time = TimeUtil.getDateTime(System.currentTimeMillis(), "yy-MM-dd HH:mm:ss")
        try {
            val contentValues = contentValuesOf(
                "packageName" to packageName,
                "log" to log,
                "read" to 0,
                "type" to type,
                "time" to time
            )
            context?.let {
                it.contentResolver?.insert(PRINT_URI, contentValues)
            }
        } catch (e: Exception) {
            "error occurred while saving log to the database, prepare to write to the file".tip()
            printLogToFile(log, packageName, type, time)
        }
    }

    private fun printLogToFile(log: String, packageName: String, type: String, time: String) {
        try {
            val printLog = PrintLog(log = log, packageName = packageName, type = type, time = time)
            val printLogStr = Gson().toJson(printLog)
            val filePath = if (FlavorUtils.isNormal()) {
                Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
            } else {
                Constant.CONFIG_MAIN_DIRECTORY + Constant.RECORD_TEMP_DIRECTORY
            }
            FileUtils.writeLogToFile(
                content = printLogStr, filePath = filePath
            )
        } catch (e: Exception) {
            "error occurred while saving log to the file, 此次log打印在下方".tip()
        }
    }

    fun toStackTrace(
        stackTrace: Array<StackTraceElement>
    ): List<String> {
        val items = mutableListOf<String>()
        var notBug = 0
        for (element in stackTrace) {
            val className = element.className
            if (className.startsWith("me.simpleHook") || className.startsWith("littleWhiteDuck") || className.startsWith(
                    "de.robv.android.xposed"
                ) || className.contains("LspHooker") || className.contains("EdHooker") || className.startsWith(
                    "me.weishu"
                )
            ) continue
            if (notBug == 0) {
                items.add("调用堆栈：")
            }
            notBug++
            items.add("类：${element.className} -->方法：${element.methodName}(line：${element.lineNumber})")
        }
        return items
    }
}