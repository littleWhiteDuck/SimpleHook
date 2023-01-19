package me.simpleHook.hook.utils

import com.google.gson.Gson
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.hook.Tip
import me.simpleHook.util.*

object LogUtil {

    fun toLogMsg(log: String, packageName: String, type: String) {
        if (type == "null") return
        val time = TimeUtil.getDateTime(System.currentTimeMillis(), "yy-MM-dd HH:mm:ss")
        val tempPackageName = if (type.startsWith("Error")) "error.hook.tip" else packageName
        try {
            val printLog =
                PrintLog(log = log, packageName = tempPackageName, type = type, time = time)
            val printLogStr = Gson().toJson(printLog)
            val filePath =
                Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
            FileUtils.writeLogToFile(content = printLogStr, filePath = filePath)
        } catch (e: Exception) {
            "error occurred while saving log to the file, 此次log打印在下方".tip(packageName)
            log.log(packageName)
        }
    }

    fun getStackTrace(): List<String> {
        val stackTrace = Throwable().stackTrace
        val isNotChinese = LanguageUtils.isNotChinese()
        val items = mutableListOf<String>()
        var notBug = 0
        for (element in stackTrace) {
            val className = element.className
            if (className.startsWith("me.simpleHook") || className.startsWith("littleWhiteDuck") || className.startsWith(
                    "de.robv.android.xposed"
                ) || className.contains(
                    "LspHooker", true
                ) || className.contains("EdHooker") || className.startsWith(
                    "me.weishu"
                )
            ) continue
            if (notBug == 0) {
                items.add(if (isNotChinese) "Call stack: " else "调用堆栈：")
            }
            notBug++
            items.add("${if (isNotChinese) "Class : " else "类："}${element.className} -->${if (isNotChinese) "Method : " else "方法："}${element.methodName}(line：${element.lineNumber})")
        }
        return items
    }


    fun toLog(
        list: List<String>, packageName: String, type: String
    ) {
        val logBean = LogBean(type = type, other = list, "error.hook.tip")
        LogUtil.toLogMsg(Gson().toJson(logBean), packageName, type)
    }

    fun notFoundClass(
        packageName: String, className: String, methodName: String, error: String
    ) {
        val list = listOf(
            Tip.getTip("errorType") + "ClassNotFoundError",
            Tip.getTip("solution") + Tip.getTip("notFoundClass"),
            Tip.getTip("filledClassName") + className,
            Tip.getTip("filledMethodOrField") + methodName,
            Tip.getTip("detailReason") + error
        )
        toLog(list, packageName, "Error ClassNotFoundError")
    }

    fun noSuchMethod(
        packageName: String, className: String, methodName: String, error: String
    ) {
        val list = listOf(
            Tip.getTip("errorType") + "NoSuchMethodError",
            Tip.getTip("solution") + Tip.getTip("useSmali2Config"),
            Tip.getTip("filledClassName") + className,
            Tip.getTip("filledMethodParams") + methodName,
            Tip.getTip("detailReason") + error
        )
        toLog(list, packageName, "Error NoSuchMethodError")
    }
}