package me.simpleHook.hook.extension

import android.app.Application
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.Exit
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.hook.util.LogUtil.outLogMsg

object ApplicationHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.application || configBean.exit.enable) {
            findMethod(Application::class.java) {
                name == "onCreate"
            }.hookAfter {
                if (configBean.application) {
                    val className = it.thisObject.javaClass.name
                    val type = "Application"
                    outLogMsg(LogBean(type,
                        listOf(getTip("applicationName") + className),
                        hostPackageName))
                }
                if (configBean.exit.enable) {
                    val exit = Json.decodeFromString<Exit>(configBean.exit.info)
                    if (exit.recordCrash) recordCrash()
                }
            }
        }

    }

    private fun recordCrash() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            t ?: return@setDefaultUncaughtExceptionHandler
            val type = if (isShowEnglish) "CrashCaught" else "错误捕获"
            val isMainThread = t.name == "main"
            val list = listOf("Thread name(线程名)：${t.name}",
                "Main thread(主线程): $isMainThread",
                e.stackTraceToString())
            outLogMsg(LogBean(type, list, hostPackageName))
        }
    }
}