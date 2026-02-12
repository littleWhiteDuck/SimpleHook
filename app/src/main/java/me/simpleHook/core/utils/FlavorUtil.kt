package me.simpleHook.core.utils

import me.simpleHook.BuildConfig

object FlavorUtil {

    const val liteVersion = BuildConfig.FLAVOR == "lite"
    const val rootVersion = BuildConfig.FLAVOR == "root"
    const val normalVersion = BuildConfig.FLAVOR == "normal"
    const val AUTHORITIES = "me.simplehook.provider." + BuildConfig.FLAVOR
    const val PROVIDER_RECORD_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/records"
    const val betaVersion = BuildConfig.IS_BETA
}