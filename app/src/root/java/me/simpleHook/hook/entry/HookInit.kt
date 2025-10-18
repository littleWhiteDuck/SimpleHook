package me.simpleHook.hook.entry

import me.simpleHook.hook.MainHook
import me.simpleHook.hook.utils.ConfigUtil
import me.simpleHook.hook.utils.xLog

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