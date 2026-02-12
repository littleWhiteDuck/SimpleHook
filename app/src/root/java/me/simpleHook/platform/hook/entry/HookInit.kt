package me.simpleHook.platform.hook.entry

import me.simpleHook.platform.hook.MainHook
import me.simpleHook.platform.hook.utils.ConfigUtil
import me.simpleHook.platform.hook.utils.xLog

object HookInit {

    fun startHook() {
        ConfigUtil.getCustomConfigFromFile()?.let {
            "get custom config succeed from file".xLog()
            MainHook.readyHook(it)
        }
        ConfigUtil.getExtConfigFromFile()?.let {
            "get extension config succeed from file".xLog()
            MainHook.readyExtensionHook(it)
        }
    }

}