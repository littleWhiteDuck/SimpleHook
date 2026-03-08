package me.simpleHook.platform.hook.entry

import me.simpleHook.platform.hook.MainHook
import me.simpleHook.platform.hook.utils.HookConfigFileUtil
import me.simpleHook.platform.hook.utils.xLog

object HookInit {

    fun startHook() {
        HookConfigFileUtil.getCustomConfigFromFile()?.let {
            "get custom config succeed from file".xLog()
            MainHook.startCustomHooks(it)
        }
        HookConfigFileUtil.getExtensionConfigFromFile()?.let {
            "get extension config succeed from file".xLog()
            MainHook.startExtensionHooks(it)
        }
    }

}
