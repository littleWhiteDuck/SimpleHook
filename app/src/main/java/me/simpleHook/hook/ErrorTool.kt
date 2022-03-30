package me.simpleHook.hook

import android.content.Context
import com.google.gson.Gson
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant

object ErrorTool {
    fun toLog(
        context: Context, list: List<String>, packageName: String
    ) {
        val type = Constant.SIMPLE_HOOK_ERROR
        val logBean = LogBean(type = type, other = list, type)
        LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
    }
}