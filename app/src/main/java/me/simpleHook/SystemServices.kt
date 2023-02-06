package me.simpleHook

import android.content.pm.PackageManager

object SystemServices {
    val packageManager: PackageManager by lazy { SimpleHookApp.app.packageManager }
}