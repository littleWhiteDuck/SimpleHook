package me.simpleHook.hook.extension

import android.content.Intent
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.ExtraBean
import me.simpleHook.bean.IntentBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil

private const val ACTIVITY = "android.app.Activity"
private const val CONTEXT_WRAPPER = "android.content.ContextWrapper"
private const val START_ACTIVITY = "startActivity"
private const val START_ACTIVITY_FOR_RESULT = "startActivityForResult"

object IntentHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
        if (!configBean.intent) return
        XposedHelpers.findAndHookMethod(ACTIVITY,
            InitFields.ezXClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, packageName)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER,
            InitFields.ezXClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, packageName)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER,
            InitFields.ezXClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, packageName)
                }
            })

        XposedHelpers.findAndHookMethod(ACTIVITY,
            InitFields.ezXClassLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, packageName)
                }
            })
        XposedHelpers.findAndHookMethod(
            ACTIVITY,
            InitFields.ezXClassLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, packageName)
                }
            })
    }


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
        val logBean = LogBean(
            "intent", listOf(configBean), packName
        )
        LogUtil.toLogMsg(Gson().toJson(logBean), packName, "intent")
    }
}