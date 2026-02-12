package me.simpleHook.platform.hook.extension

import me.simpleHook.data.ExtensionConfig


abstract class BaseHook {

    var isInit = false
    abstract fun startHook(extensionConfig: ExtensionConfig)
}