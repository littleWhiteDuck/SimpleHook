package me.simpleHook.hook.utils

import android.net.Uri
import android.os.Build.VERSION_CODES
import androidx.core.content.contentValuesOf
import com.google.gson.Gson
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.hook.Tip
import me.simpleHook.hook.utils.HookHelper.hostPackageName
import me.simpleHook.util.*

object LogUtil {
    private const val filterClass =
        """(?i)EdHooker|LspHooker|littleWhiteDuck|me.simpleHook|me.weishu|de.robv.android.xposed"""
    private val PRINT_URI = Uri.parse("content://me.simplehook.provider/print_logs")
    fun toLogMsg(log: String, type: String) {
        if (type == "null" || !HookHelper.enableRecord) return
        HookHelper.appContext.getExternalFilesDirs("")
        val time = TimeUtil.getDateTime(System.currentTimeMillis(), "yy-MM-dd HH:mm:ss")
        val tempPackageName = if (type.startsWith("Error")) "error.hook.tip" else hostPackageName
        if (FlavorUtils.isLiteVersion) {
            log.log(hostPackageName)
        } else if (HookHelper.appInfo.targetSdkVersion > VERSION_CODES.Q) {
            outLogDB(log, tempPackageName, type, time)
        } else {
            outLogFile(log, tempPackageName, type, time)
        }

    }

    private fun outLogFile(
        log: String, tempPackageName: String, type: String, time: String
    ) {
        try {
            val printLog =
                PrintLog(log = log, packageName = tempPackageName, type = type, time = time)
            val printLogStr = Gson().toJson(printLog)
            val filePath =
                Constant.ANDROID_DATA_PATH + hostPackageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
            FileUtils.writeLogToFile(content = printLogStr, filePath = filePath)
        } catch (e: Exception) {
            "error occurred while saving log to the file, 此次log打印在下方".tip(hostPackageName)
            log.log(hostPackageName)
        }
    }

    private fun outLogDB(
        log: String, tempPackageName: String, type: String, time: String
    ) {
        try {
            val contentValues = contentValuesOf(
                "packageName" to tempPackageName,
                "log" to log,
                "read" to 0,
                "type" to type,
                "time" to time,
                "isMark" to 0
            )
            HookHelper.appContext.contentResolver?.insert(PRINT_URI, contentValues)
        } catch (e: Exception) {
            "error occurred while saving log to the database".tip(hostPackageName)
            outLogFile(log, tempPackageName, type, time)
        }
    }

    fun getStackTrace(): List<String> {
        val stackTrace = Throwable().stackTrace
        val isNotChinese = LanguageUtils.isNotChinese()
        val items = mutableListOf<String>()
        var notBug = 0
        for (element in stackTrace) {
            val className = element.className
            if (className.contains(Regex(filterClass))) continue
            if (notBug == 0) {
                items.add(if (isNotChinese) "Call stack: " else "调用堆栈：")
            }
            notBug++
            items.add("${if (isNotChinese) "Class : " else "类："}${element.className} -->${if (isNotChinese) "Method : " else "方法："}${element.methodName}(line：${element.lineNumber})")
        }
        return items
    }


    fun toLog(
        list: List<String>, type: String
    ) {
        val logBean = LogBean(type = type, other = list, "error.hook.tip")
        toLogMsg(Gson().toJson(logBean), type)
    }

    fun notFoundClass(
        className: String, methodName: String, error: String
    ) {
        val list = listOf(
            Tip.getTip("errorType") + "ClassNotFoundError",
            Tip.getTip("solution") + Tip.getTip("notFoundClass"),
            Tip.getTip("filledClassName") + className,
            Tip.getTip("filledMethodOrField") + methodName,
            Tip.getTip("detailReason") + error
        )
        toLog(list, "Error ClassNotFoundError")
    }

    fun noSuchMethod(
        className: String, methodName: String, error: String
    ) {
        val list = listOf(
            Tip.getTip("errorType") + "NoSuchMethodError",
            Tip.getTip("solution") + Tip.getTip("useSmali2Config"),
            Tip.getTip("filledClassName") + className,
            Tip.getTip("filledMethodParams") + methodName,
            Tip.getTip("detailReason") + error
        )
        toLog(list, "Error NoSuchMethodError")
    }
}