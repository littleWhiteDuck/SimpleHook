package me.simpleHook.util

import android.os.Build

object FlavorUtils {

    fun isNormal(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
    }
}