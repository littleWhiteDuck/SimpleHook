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
import me.simpleHook.constant.Constant
import me.simpleHook.hook.util.ConfigUtil
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.extension.log

class HookInit : IXposedHookLoadPackage {

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
                HookHelper.appContext.getExternalFilesDirs(null)
                startHook()
            }
        }
    }

    private fun startHook() {
        val packageName = HookHelper.hostPackageName
        ConfigUtil.getConfigFromFile()?.let {
            "get custom config succeed from file".log(packageName)
            MainHook.readyHook(it)
        } ?: run {
            "get custom config failed from file".log(packageName)
            ConfigUtil.getCustomConfigFromDB()?.let {
                "get custom config succeed from db".log(packageName)
                MainHook.readyHook(it)
            } ?: "get custom config failed from db".log(packageName)
        }
        ConfigUtil.getConfigFromFile(Constant.EXTENSION_CONFIG_NAME)?.let {
            "get extension config succeed from file".log(packageName)
            MainHook.readyExtensionHook(it)
        } ?: run {
            "get extension config failed from file".log(packageName)
            ConfigUtil.getExConfigFromDB()?.let {
                "get extension config succeed from db".log(packageName)
                MainHook.readyExtensionHook(it)
            } ?: "get extension config failed from db".log(packageName)
        }
    }

}