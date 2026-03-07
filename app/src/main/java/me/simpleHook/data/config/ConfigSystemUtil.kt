package me.simpleHook.data.config

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
        if (GlobalValue.isRootWork) return SuConfigHelper()
        if (GlobalValue.isShizukuWork) return ShizukuConfigHelper()
        return when {
            OSUtil.atLeastR() -> DocumentConfigHelper()
            else -> FileConfigHelper()
        }
    }
}
