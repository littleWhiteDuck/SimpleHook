package me.simpleHook.hook.extension

import android.provider.Settings.Global
import android.provider.Settings.Secure
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import me.simpleHook.bean.ExtensionConfig

object ADBHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.adb) {
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

    private fun disableCheckAdb(it: XC_MethodHook.MethodHookParam) {
        val keyName = it.args[1] as String
        if (keyName == Global.ADB_ENABLED) {
            it.result = 0
        }
    }
}