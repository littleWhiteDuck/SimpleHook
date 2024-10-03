package me.simpleHook.hook.entry

import de.robv.android.xposed.XSharedPreferences
import me.simpleHook.BuildConfig
import me.simpleHook.constant.Constant
import me.simpleHook.hook.MainHook
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.hook.util.log

object HookInit {

    private val prefHookConfig by lazy { getPref(Constant.CUSTOM_CONFIG_PREF) }

    fun startHook() {
        prefHookConfig?.let { sp ->
            sp.getString(hostPackageName, null)?.let {
                MainHook.readyHook(it)
            } ?: "not have the custom config".log()
        } ?: "null: XSharedPreferences".log()
    }


    private fun getPref(path: String): XSharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
        return if (pref.file.canRead()) pref else null
    }

}