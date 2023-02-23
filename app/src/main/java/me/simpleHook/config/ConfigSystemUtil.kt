package me.simpleHook.config

import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.OSUtils


object ConfigSystemUtil {

    fun getConfigSystem(): ConfigSystem {
        if (FlavorUtils.liteVersion) return PrefConfigHelper()
        if (FlavorUtils.rootVersion) return SuConfigHelper()
        return when {
            OSUtils.atLeastT() -> DocumentConfig2Helper()
            OSUtils.atLeastR() -> DocumentConfigHelper()
            else -> FileConfigHelper()
        }
    }
}