package me.simpleHook

import android.content.pm.PackageManager
import me.simpleHook.util.SPUtils

object GlobalServices {
    val packageManager: PackageManager by lazy { App.app.packageManager }
    val sp by lazy { SPUtils(App.app) }
}