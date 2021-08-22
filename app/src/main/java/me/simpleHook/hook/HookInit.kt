package me.simpleHook.hook

import android.content.SharedPreferences
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import littleWhiteDuck.readyHook
import me.simpleHook.BuildConfig

class HookInit : IXposedHookLoadPackage {

    companion object {
        private fun getPref(path: String = "hookConfig"): SharedPreferences? {
            val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
            return if (pref.file.canRead()) pref else null
        }

        val prefForA by lazy { getPref() }

    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        readyHook(lpparam, prefForA)
    }

}