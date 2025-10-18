package me.simpleHook.hook.extension

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import kotlinx.serialization.json.Json
import me.simpleHook.data.Exit
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.hook.utils.RecordOutHelper

object ExitHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.exit.enable) {
            val exit = Json.decodeFromString<Exit>(extensionConfig.exit.info)
            if (exit.exit) {
                findMethod(Runtime::class.java) {
                    name == "exit"
                }.hookReplace {
                    RecordOutHelper.outputExitRecord(type = "exit")
                }
            }
            if (exit.kill) {
                findMethod(android.os.Process::class.java) {
                    name == "killProcess"
                }.hookReplace {
                    RecordOutHelper.outputExitRecord(type = "killProcess")
                }
            }
            if (exit.finish) {
                findMethod(Activity::class.java) {
                    name == "finish"
                }.hookReplace {
                    RecordOutHelper.outputExitRecord(type = "finish")
                }
            }
        }
    }
}