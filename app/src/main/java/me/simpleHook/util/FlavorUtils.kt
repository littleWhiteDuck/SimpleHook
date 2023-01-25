package me.simpleHook.util

import android.os.Build
import me.simpleHook.BuildConfig

object FlavorUtils {

    fun isNormal(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
    }

    const val isLiteVersion = BuildConfig.FLAVOR == "lite"
}