package me.simpleHook.config

import me.simpleHook.GlobalValue
import me.simpleHook.utils.FlavorUtil
import me.simpleHook.utils.OSUtil


object ConfigSystemUtil {

    fun getConfigSystem(): ConfigSystem {
        if (FlavorUtil.liteVersion) return PrefConfigHelper()
        if (FlavorUtil.rootVersion) {
            return if (GlobalValue.isRootWork) {
                SuConfigHelper()
            } else {
                ShizukuConfigHelper()
            }
        }
        return when {
            OSUtil.atLeastT() -> DocumentConfig2Helper()
            OSUtil.atLeastR() -> DocumentConfigHelper()
            else -> FileConfigHelper()
        }
    }
}