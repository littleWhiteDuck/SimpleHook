package me.simpleHook.hook

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.BuildConfig

class HookInit : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelperInit.initHandleLoadPackage(lpparam)
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            findMethod("me.simpleHook.ui.activity.MainActivity") {
                name == "isModuleLive"
            }.hookReturnConstant(true)

        } else {
            findMethod(Application::class.java) {
                name == "attach"
            }.hookAfter {
                EzXHelperInit.initAppContext(context = it.args[0] as Context)
                MainHook.startHook(lpparam.packageName)
            }
        }
    }
}