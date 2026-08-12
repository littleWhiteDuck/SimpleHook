package me.simpleHook.core.utils

import me.simpleHook.BuildConfig

object FlavorUtil {

    const val rootVersion = BuildConfig.FLAVOR == "root"
    const val AUTHORITIES = "me.simplehook.provider." + BuildConfig.FLAVOR
    const val PROVIDER_RECORD_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/records"
}
