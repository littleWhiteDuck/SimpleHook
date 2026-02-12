package me.simpleHook.platform.hook.extension

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.RecordOutHelper

object ExitHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.exitConfig.enable) {
            val exitConfig = extensionConfig.exitConfig
            if (exitConfig.exit) {
                findMethod(Runtime::class.java) {
                    name == "exit"
                }.hookReplace {
                    RecordOutHelper.outputExitRecord(type = "exit")
                }
            }
            if (exitConfig.kill) {
                findMethod(android.os.Process::class.java) {
                    name == "killProcess"
                }.hookReplace {
                    RecordOutHelper.outputExitRecord(type = "killProcess")
                }
            }
            if (exitConfig.finish) {
                findMethod(Activity::class.java) {
                    name == "finish"
                }.hookReplace {
                    RecordOutHelper.outputExitRecord(type = "finish")
                }
            }
        }
    }
}