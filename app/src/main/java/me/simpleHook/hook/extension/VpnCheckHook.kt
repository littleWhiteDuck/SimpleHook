package me.simpleHook.hook.extension

import com.github.kyuubiran.ezxhelper.init.InitFields
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean

object VpnCheckHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
        if (!configBean.vpn) return
        XposedHelpers.findAndHookMethod("java.net.NetworkInterface",
            InitFields.ezXClassLoader,
            "getName",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    param.result = "are you ok"
                }
            })
    }
}