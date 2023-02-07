package me.simpleHook.hook.extension

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.utils.HookHelper
import me.simpleHook.hook.utils.LogUtil

object ApplicationHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean) {
        if (!configBean.application) return
        XposedHelpers.findAndHookMethod(
            Application::class.java, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val className = param.thisObject.javaClass.name
                    val type = "Application"
                    LogUtil.toLogMsg(
                        LogBean(
                            type,
                            listOf(getTip("applicationName") + className),
                            HookHelper.hostPackageName
                        )
                    )
                }
            })
    }
}