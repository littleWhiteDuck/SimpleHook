package me.simpleHook.hook

import android.content.Context
import me.simpleHook.util.LanguageUtils


abstract class BaseHook(val mClassLoader: ClassLoader, val mContext: Context) {

    protected val isShowEnglish = LanguageUtils.isNotChinese()

    abstract fun startHook(packageName: String, strConfig: String)
}