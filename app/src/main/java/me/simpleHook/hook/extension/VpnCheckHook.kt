package me.simpleHook.hook.extension

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.hook.utils.HookHelper

object VpnCheckHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean) {
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