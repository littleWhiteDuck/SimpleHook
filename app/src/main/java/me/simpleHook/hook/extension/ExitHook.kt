package me.simpleHook.hook.extension

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import kotlinx.serialization.json.Json
import me.simpleHook.data.Exit
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.LogBean
import me.simpleHook.hook.language.tip
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
                    outLog(tip.exit)
                }
            }
            if (exit.kill) {
                findMethod(android.os.Process::class.java) {
                    name == "killProcess"
                }.hookReplace {
                    outLog(tip.killProcess)
                }
            }
            if (exit.finish) {
                findMethod(Activity::class.java) {
                    name == "finish"
                }.hookReplace {
                    outLog(tip.finish)
                }
            }
        }
    }

    private fun outLog(tip: String) {
        val type = if (isShowEnglish) "Exit" else "退出"
        LogUtil.outLogMsg(
            LogBean(
                type,
                listOf(tip) + LogUtil.getStackTrace(),
                HookHelper.hostPackageName
            )
        )
    }
}