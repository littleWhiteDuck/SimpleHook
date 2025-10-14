package me.simpleHook.hook.extension

import me.simpleHook.data.ExtensionConfig
import me.simpleHook.utils.LanguageUtil


abstract class BaseHook {

    protected val isShowEnglish = LanguageUtil.isNotChinese()
    var isInit = false
    abstract fun startHook(configBean: ExtensionConfig)
}