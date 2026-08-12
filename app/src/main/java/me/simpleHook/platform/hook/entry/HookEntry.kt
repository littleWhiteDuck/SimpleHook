package me.simpleHook.platform.hook.entry

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import me.simpleHook.BuildConfig
import me.simpleHook.platform.hook.utils.HookHelper

object HookEntry {
    fun handleLoadPackage(packageName: String) {
        if (packageName == BuildConfig.APPLICATION_ID) {
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
                    packageName = packageName,
                    applicationName = it.thisObject.javaClass.name
                )
                HookHelper.appContext.getExternalFilesDirs(null)
                HookInit.startHook()
            }
        }
    }
}