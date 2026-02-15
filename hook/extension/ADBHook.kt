package me.simpleHook.platform.hook.extension

import android.provider.Settings.Global
import android.provider.Settings.Secure
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.util.xpcompat.XC_MethodHook
import me.simpleHook.data.ExtensionConfig

object ADBHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.adb) {
            findAllMethods(Secure::class.java) {
                name == "getInt"
            }.hookBefore {
                disableCheckAdb(it)
            }
            findAllMethods(Global::class.java) {
                name == "getInt"
            }.hookBefore {
                disableCheckAdb(it)
            }
        }
    }

    private fun disableCheckAdb(param: XC_MethodHook.MethodHookParam) {
        val keyName = param.args[1] as String
        if (keyName == Global.ADB_ENABLED) {
            param.result = 0
        }
    }
}