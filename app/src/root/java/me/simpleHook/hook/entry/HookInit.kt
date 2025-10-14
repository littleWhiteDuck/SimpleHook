package me.simpleHook.hook.entry

import me.simpleHook.hook.MainHook
import me.simpleHook.hook.utils.ConfigUtil
import me.simpleHook.hook.utils.log

object HookInit {

    fun startHook() {
        ConfigUtil.getCustomConfigFromFile()?.let {
            "get custom config succeed from file".log()
            MainHook.readyHook(it)
        } ?: run {
            "get custom config failed from file".log()
            ConfigUtil.getCustomConfigFromDB()?.let {
                "get custom config succeed from db".log()
                MainHook.readyHook(it)
            } ?: "get custom config failed from db".log()
        }
        ConfigUtil.getExtConfigFromFile()?.let {
            "get extension config succeed from file".log()
            MainHook.readyExtensionHook(it)
        } ?: run {
            "get extension config failed from file".log()
            ConfigUtil.getExConfigFromDB()?.let {
                "get extension config succeed from db".log()
                MainHook.readyExtensionHook(it)
            } ?: "get extension config failed from db".log()
        }
    }

}