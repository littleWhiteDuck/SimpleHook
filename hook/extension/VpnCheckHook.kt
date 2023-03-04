package me.simpleHook.hook.extension

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.hook.util.HookHelper

object VpnCheckHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.vpn) return
        XposedHelpers.findAndHookMethod("java.net.NetworkInterface",
            HookHelper.appClassLoader,
            "getName",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    param.result = "are you ok"
                }
            })
    }
}