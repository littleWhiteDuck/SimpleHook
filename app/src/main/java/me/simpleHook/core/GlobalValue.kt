package me.simpleHook.core

import android.content.pm.PackageManager
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.core.utils.SPUtil

object GlobalValue {
    private const val WORK_MODE_ROOT = "Root"
    private const val WORK_MODE_SHIZUKU = "Shizuku"
    private const val WORK_MODE_NORMAL = "Normal"

    val packageManager: PackageManager by lazy { App.packageManager }
    val sp by lazy { SPUtil(App) }

    val isShizukuWork
        get() = FlavorUtil.rootVersion && normalizeWorkMode(sp.workMode) == WORK_MODE_SHIZUKU

    val isRootWork
        get() = FlavorUtil.rootVersion && normalizeWorkMode(sp.workMode) == WORK_MODE_ROOT

    val isNormalWork
        get() = !FlavorUtil.rootVersion || normalizeWorkMode(sp.workMode) == WORK_MODE_NORMAL

    private fun normalizeWorkMode(mode: String?): String {
        return when (mode?.trim()?.lowercase()) {
            WORK_MODE_ROOT.lowercase() -> WORK_MODE_ROOT
            WORK_MODE_SHIZUKU.lowercase() -> WORK_MODE_SHIZUKU
            WORK_MODE_NORMAL.lowercase() -> WORK_MODE_NORMAL
            else -> WORK_MODE_ROOT
        }
    }

}
