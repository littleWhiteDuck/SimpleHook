package me.simpleHook.platform.hook.entry

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.qauxv.loader.sbl.xp51.Xp51HookImpl
import io.github.qauxv.poststartup.StartupInfo
import me.simpleHook.BuildConfig
import me.simpleHook.platform.hook.utils.HookHelper

class Xp51HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelperInit.initHandleLoadPackage(lpparam)
        StartupInfo.setHookBridge(Xp51HookImpl.INSTANCE)
        StartupInfo.setLoaderService(Xp51HookImpl.INSTANCE)

        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            findMethod("me.simpleHook.feature.main.ui.MainActivity") {
                name == "isModuleLive"
            }.hookReturnConstant(true)
        } else {
            if (HookHelper.isAppContextInitialized) return
            findMethod(Application::class.java) {
                name == "attach"
            }.hookAfter {
                HookHelper.initFields(
                    context = it.args[0] as Context,
                    packageName = lpparam.packageName,
                    applicationName = it.thisObject.javaClass.name
                )
                HookHelper.appContext.getExternalFilesDirs(null)
                HookInit.startHook()
            }
        }
    }
}