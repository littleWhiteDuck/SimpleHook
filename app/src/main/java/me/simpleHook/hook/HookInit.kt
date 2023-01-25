package me.simpleHook.hook

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.BuildConfig
import me.simpleHook.hook.utils.HookHelper

class HookInit : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelperInit.initHandleLoadPackage(lpparam)
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            findMethod("me.simpleHook.ui.activity.MainActivity") {
                name == "isModuleLive"
            }.hookReturnConstant(true)

        } else {
            if (HookHelper.isAppContextInitialized) return
            findMethod(Application::class.java) {
                name == "attach"
            }.hookAfter {
                HookHelper.initFields(context = it.args[0] as Context, lpparam)
                MainHook.startHook(lpparam.packageName)
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

}