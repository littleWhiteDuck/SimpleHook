package me.simpleHook.platform.hook.entry

import de.robv.android.xposed.XSharedPreferences
import me.simpleHook.BuildConfig
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.platform.hook.MainHook
import me.simpleHook.platform.hook.utils.HookConfigFileUtil
import me.simpleHook.platform.hook.utils.HookHelper.hostPackageName
import me.simpleHook.platform.hook.utils.xLog

object HookInit {

    fun startHook() {
        if (FlavorUtil.liteVersion) {
            getPrefConfig(Constant.CUSTOM_CONFIG_PREF)?.let {
                "get custom config succeed from shared prefs".xLog()
                MainHook.startCustomHooks(it)
            }
            getPrefConfig(Constant.EXTENSION_CONFIG_PREF)?.let {
                "get extension config succeed from shared prefs".xLog()
                MainHook.startExtensionHooks(it)
            }
            return
        }
        HookConfigFileUtil.getCustomConfigFromFile()?.let {
            "get custom config succeed from file".xLog()
            MainHook.startCustomHooks(it)
        }
        HookConfigFileUtil.getExtensionConfigFromFile()?.let {
            "get extension config succeed from file".xLog()
            MainHook.startExtensionHooks(it)
        }
    }

    private fun getPrefConfig(name: String): String? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, name)
        if (!pref.file.canRead()) return null
        pref.reload()
        return pref.getString(hostPackageName, null)
    }

}