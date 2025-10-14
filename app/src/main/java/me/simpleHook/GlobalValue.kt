package me.simpleHook

import android.content.pm.PackageManager
import me.simpleHook.utils.SPUtil

object GlobalValue {
    val packageManager: PackageManager by lazy { App.packageManager }
    val sp by lazy { SPUtil(App) }

    val isRootWork
        get() = sp.workMode == "Root"

}