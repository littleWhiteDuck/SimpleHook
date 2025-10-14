package me.simpleHook.config

import me.simpleHook.GlobalValue
import me.simpleHook.utils.FlavorUtil
import me.simpleHook.utils.OSUtils


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
            OSUtils.atLeastT() -> DocumentConfig2Helper()
            OSUtils.atLeastR() -> DocumentConfigHelper()
            else -> FileConfigHelper()
        }
    }
}