package me.simpleHook.hook.extension

import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.util.LanguageUtils


abstract class BaseHook {

    protected val isShowEnglish = LanguageUtils.isNotChinese()
    var isInit = false
    abstract fun startHook(configBean: ExtensionConfigBean)
}