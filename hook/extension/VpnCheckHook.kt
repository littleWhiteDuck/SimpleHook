package me.simpleHook.hook.extension

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.hook.BaseHook

class VpnCheckHook(classLoader: ClassLoader, context: Context) : BaseHook(classLoader, context) {

    override fun startHook(packageName: String, strConfig: String) {
        mContext.packageName
        XposedHelpers.findAndHookMethod("java.net.NetworkInterface",
            mClassLoader,
            "getName",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    param.result = "are you ok"
                }
            })
    }
}