package me.simpleHook.hook.extension

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.Exit
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil

object ExitHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.exit.enable) {
            val exit = Json.decodeFromString<Exit>(configBean.exit.info)
            if (exit.exit) {
                findMethod(Runtime::class.java) {
                    name == "exit"
                }.hookReplace {
                    outLog(Tip.getTip("exit"))
                }
            }
            if (exit.kill) {
                findMethod(android.os.Process::class.java) {
                    name == "killProcess"
                }.hookReplace {
                    outLog(Tip.getTip("killProcess"))
                }
            }
            if (exit.finish) {
                findMethod(Activity::class.java) {
                    name == "finish"
                }.hookReplace {
                    outLog(Tip.getTip("finish"))
                }
            }
        }
    }

    private fun outLog(tip: String) {
        val type = if (isShowEnglish) "Exit" else "退出"
        LogUtil.outLogMsg(LogBean(type,
            listOf(tip) + LogUtil.getStackTrace(),
            HookHelper.hostPackageName))
    }
}