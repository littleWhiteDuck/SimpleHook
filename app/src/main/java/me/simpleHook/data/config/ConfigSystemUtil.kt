package me.simpleHook.data.config

import me.simpleHook.data.local.configstore.DocumentConfig2Helper
import me.simpleHook.data.local.configstore.DocumentConfigHelper
import me.simpleHook.data.local.configstore.FileConfigHelper
import me.simpleHook.data.local.configstore.PrefConfigHelper
import me.simpleHook.data.local.configstore.ShizukuConfigHelper
import me.simpleHook.data.local.configstore.SuConfigHelper
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.core.utils.OSUtil


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