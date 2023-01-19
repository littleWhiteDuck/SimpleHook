package me.simpleHook.hook.extension

import android.app.Application
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.hook.Tip.getTip

object ApplicationHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
        if (!configBean.application) return
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
                    LogUtil.toLogMsg(log, packageName, type)
                }
            })
    }
}