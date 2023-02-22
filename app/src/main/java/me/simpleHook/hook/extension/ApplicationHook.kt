package me.simpleHook.hook.extension

import android.app.Application
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.hook.util.LogUtil.outLogMsg

object ApplicationHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.application) return
        findMethod(Application::class.java) {
            name == "onCreate"
        }.hookAfter {
            val className = it.thisObject.javaClass.name
            val type = "Application"
            outLogMsg(LogBean(type, listOf(getTip("applicationName") + className), hostPackageName))
        }
    }
}