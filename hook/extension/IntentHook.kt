package me.simpleHook.hook.extension

import android.content.Intent
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.ExtraBean
import me.simpleHook.bean.IntentBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil

private const val ACTIVITY = "android.app.Activity"
private const val CONTEXT_WRAPPER = "android.content.ContextWrapper"
private const val START_ACTIVITY = "startActivity"
private const val START_ACTIVITY_FOR_RESULT = "startActivityForResult"

object IntentHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.intent) return
        XposedHelpers.findAndHookMethod(ACTIVITY,
            HookHelper.appClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, HookHelper.hostPackageName)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER,
            HookHelper.appClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, HookHelper.hostPackageName)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER,
            HookHelper.appClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, HookHelper.hostPackageName)
                }
            })

        XposedHelpers.findAndHookMethod(ACTIVITY,
            HookHelper.appClassLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, HookHelper.hostPackageName)
                }
            })
        XposedHelpers.findAndHookMethod(ACTIVITY,
            HookHelper.appClassLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, HookHelper.hostPackageName)
                }
            })
    }

    @Suppress("DEPRECATION")
    fun saveLog(intent: Intent, packName: String) {
        val className = intent.component?.className ?: ""
        val packageName = intent.component?.packageName ?: ""
        val action = intent.action ?: ""
        val data = intent.dataString ?: ""
        val extraList = ArrayList<ExtraBean>()
        val extras = intent.extras
        extras?.keySet()?.forEach {
            val type = when (extras.get(it)) {
                is Boolean -> "boolean"
                is String -> "string"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is Bundle -> "bundle"
                else -> "暂未统计" // maybe error
            }
            extraList.add(ExtraBean(type, it, extras.get(it).toString()))
        }
        val configBean = IntentBean(packageName, className, action, data, extraList)
        val logBean = LogBean("intent", listOf(Json.encodeToString(configBean)), packName)
        LogUtil.outLogMsg(logBean)
    }
}