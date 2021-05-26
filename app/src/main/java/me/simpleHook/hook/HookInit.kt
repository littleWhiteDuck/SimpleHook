package me.simpleHook.hook

import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import littleWhiteDuck.readyHook

class HookInit : IXposedHookLoadPackage {

    @Keep
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        readyHook(lpparam)
    }
}