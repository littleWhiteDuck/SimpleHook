package me.simpleHook.hook

import android.app.Application
import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.BuildConfig
import me.simpleHook.util.log

class HookInit : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            XposedHelpers.findAndHookMethod("me.simpleHook.ui.activity.MainActivity",
                lpparam.classLoader,
                "isModuleLive",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        "激活(active)".log(BuildConfig.APPLICATION_ID)
                        param.result = true
                    }
                })
        } else {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mContext = param.args[0] as Context
                        val mClassLoader = mContext.classLoader
                        MainHook(mClassLoader, mContext).startHook(
                            packageName = lpparam.packageName, ""
                        )
                    }
                })
        }
    }
}