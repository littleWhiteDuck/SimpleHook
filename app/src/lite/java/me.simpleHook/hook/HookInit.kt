package me.simpleHook.hook

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.BuildConfig
import me.simpleHook.constant.Constant
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.util.log

class HookInit : IXposedHookLoadPackage {

    private val prefHookConfig by lazy { getPref(Constant.CUSTOM_CONFIG_PREF) }

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
                readyXmlHook()
            }
        }
    }

    private fun readyXmlHook() {
        prefHookConfig?.let { sp ->
            sp.getString(hostPackageName, null)?.let {
                MainHook.readyHook(it)
            } ?: "not have the custom config".log(hostPackageName)
        } ?: "null: XSharedPreferences".log(hostPackageName)
    }


    private fun getPref(path: String): XSharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
        return if (pref.file.canRead()) pref else null
    }

}