package me.simpleHook.platform.hook.extension

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.HookHelper

object VpnCheckHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.vpn) return
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