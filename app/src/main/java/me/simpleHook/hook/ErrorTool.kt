package me.simpleHook.hook

import android.content.Context
import com.google.gson.Gson
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip.getTip

object ErrorTool {

    @JvmStatic
    fun toLog(
        context: Context, list: List<String>, packageName: String, type: String
    ) {
        val logBean = LogBean(type = type, other = list, "error.hook.tip")
        LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
    }

    @JvmStatic
    fun notFoundClass(
        context: Context, packageName: String, className: String, methodName: String, error: String
    ) {
        val list = listOf(
            getTip("errorType") + "ClassNotFoundError",
            getTip("solution") + getTip("notFoundClass"),
            getTip("filledClassName") + className,
            getTip("filledMethodOrField") + methodName,
            getTip("detailReason") + error
        )
        toLog(context, list, packageName, "Error ClassNotFoundError")
    }

    @JvmStatic
    fun noSuchMethod(
        context: Context, packageName: String, className: String, methodName: String, error: String
    ) {
        val list = listOf(
            getTip("errorType") + "NoSuchMethodError",
            getTip("solution") + getTip("useSmali2Config"),
            getTip("filledClassName") + className,
            getTip("filledMethodParams") + methodName,
            getTip("detailReason") + error
        )
        toLog(context, list, packageName, "Error NoSuchMethodError")
    }
}