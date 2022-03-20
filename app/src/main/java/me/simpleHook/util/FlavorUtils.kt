package me.simpleHook.util

import me.simpleHook.BuildConfig

object FlavorUtils {

    fun isNormal(): Boolean {
        return BuildConfig.normal
    }
}