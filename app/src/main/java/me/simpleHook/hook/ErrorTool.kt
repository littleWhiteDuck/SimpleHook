package me.simpleHook.hook

import android.content.Context
import com.google.gson.Gson
import me.simpleHook.bean.LogBean

object ErrorTool {
    fun toLog(
        context: Context, list: List<String>, packageName: String, type: String
    ) {
        val logBean = LogBean(type = type, other = list, "error.hook.tip")
        LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
    }

    fun notFoundClass(
        context: Context, packageName: String, className: String, methodName: String, error: String
    ) {
        val list = listOf(
            "错误类型：ClassNotFoundError",
            "解决方案：请确保你填写的类名正确",
            "所填类名：$className",
            "所填方法(参数)|变量名：$methodName",
            "具体原因：$error"
        )
        toLog(context, list, packageName, "Error ClassNotFoundError")
    }

    fun noSuchMethod(
        context: Context, packageName: String, className: String, methodName: String, error: String
    ) {
        val list = listOf(
            "错误类型：NoSuchMethodError",
            "解决方案：使用smali转配置",
            "所填类名：$className",
            "所填方法(参数)：$methodName",
            "具体原因：$error"
        )
        toLog(context, list, packageName, "Error NoSuchMethodError")
    }
}