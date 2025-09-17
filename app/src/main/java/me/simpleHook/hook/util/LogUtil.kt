package me.simpleHook.hook.util

import android.os.Build.VERSION_CODES
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.serialization.json.Json
import me.simpleHook.constant.Constant
import me.simpleHook.data.LogBean
import me.simpleHook.database.entity.PrintLog
import me.simpleHook.hook.language.tip
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.util.FileUtils
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.FlavorUtils.PROVIDER_RECORD_URI
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.TimeUtil

object LogUtil {
    private const val filterClass =
        """(?i)EdHooker|LspHooker|littleWhiteDuck|me.simpleHook|me.weishu|de.robv.android.xposed|XposedBridge"""
    private val PRINT_URI = PROVIDER_RECORD_URI.toUri()
    fun outLogMsg(logBean: LogBean) {
        if (logBean.type == "null" || !HookHelper.enableRecord) return
        val log = Json.encodeToString(logBean)
        HookHelper.appContext.getExternalFilesDirs("")
        val time = TimeUtil.getTime(System.currentTimeMillis(), "yy-MM-dd HH:mm:ss")
        val tempPackageName =
            if (logBean.type.startsWith("Error")) "error.hook.tip" else hostPackageName
        if (FlavorUtils.liteVersion) {
            log.log()
        } else if (HookHelper.appInfo.targetSdkVersion > VERSION_CODES.Q) {
            outLogFile(log, tempPackageName, logBean.type, time)
        } else {
            outLogDB(log, tempPackageName, logBean.type, time)
        }

    }

    private fun outLogFile(
        log: String, tempPackageName: String, type: String, time: String
    ) {
        try {
            val printLog =
                PrintLog(log = log, packageName = tempPackageName, type = type, time = time)
            val printLogStr = Json.encodeToString(printLog)
            val filePath =
                Constant.ANDROID_DATA_PATH + hostPackageName + "/simpleHook/" + Constant.RECORD_TEMP_DIRECTORY
            FileUtils.outTextToFile(
                filePath,
                printLogStr,
                isNewLine = true,
                limitSize = 4096,
                append = true
            )
        } catch (_: Exception) {
            "error occurred while saving log to the file, 此次log打印在下方".log()
            log.log()
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
        } catch (_: Exception) {
            "error occurred while saving log to the database".log()
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


    fun outLog(
        list: List<String>, type: String
    ) {
        val logBean = LogBean(type = type, other = list, "error.hook.tip")
        outLogMsg(logBean)
    }

    private fun notFoundClass(
        className: String, methodName: String, error: String
    ) {
        tip.notFoundClass.log()
        val list = listOf(
            tip.errorType + "ClassNotFoundError",
            tip.solution + tip.notFoundClass,
            tip.filledClassName + className,
            tip.filledMethodOrField + methodName,
            tip.detailReason + error
        )
        outLog(list, "Error ClassNotFoundError")
    }

    private fun noSuchMethod(
        className: String, methodName: String, error: String
    ) {
        tip.noSuchMethod.log()
        val list = listOf(
            tip.errorType + "NoSuchMethodError",
            tip.solution + tip.useSmali2Config,
            tip.filledClassName + className,
            tip.filledMethodParams + methodName,
            tip.detailReason + error
        )
        outLog(list, "Error NoSuchMethodError")
    }

    fun outHookError(className: String, methodName: String, exception: Throwable) {
        when (exception) {
            is NoSuchMethodError, is NoSuchMethodException -> {
                noSuchMethod(className, methodName, exception.stackTraceToString())
            }

            is XposedHelpers.ClassNotFoundError, is ClassNotFoundException -> {
                notFoundClass(className, methodName, exception.stackTraceToString())
            }
        }
    }
}