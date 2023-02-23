package me.simpleHook.util

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object OSUtils {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun atLeastP(): Boolean {
        return Build.VERSION.SDK_INT >= 28
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun atLeastR(): Boolean {
        return Build.VERSION.SDK_INT >= 30
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun atR2T(): Boolean {
        return Build.VERSION.SDK_INT in 30..32
    }

    fun atMostQ(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun atLeastO(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun atLeastT(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }
}