package me.simpleHook.hook.extension

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip.getTip

class ApplicationHook(mClassLoader: ClassLoader, mContext: Context) :
    BaseHook(mClassLoader, mContext) {

    override fun startHook(packageName: String, strConfig: String) {
        XposedHelpers.findAndHookMethod(
            Application::class.java, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val className = param.thisObject.javaClass.name
                    val type = "Application"
                    val log = Gson().toJson(
                        LogBean(
                            type, listOf(getTip("applicationName") + className), packageName
                        )
                    )
                    LogHook.toLogMsg(mContext, log, packageName, type)
                }
            })
    }
}