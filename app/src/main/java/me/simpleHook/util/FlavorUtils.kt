package me.simpleHook.util

import android.os.Build
import me.simpleHook.BuildConfig

object FlavorUtils {

    const val liteVersion = BuildConfig.FLAVOR == "lite"
    const val rootVersion = BuildConfig.FLAVOR == "root"
    const val normalVersion = BuildConfig.FLAVOR == "normal"
    const val AUTHORITIES = "me.simplehook.provider." + BuildConfig.FLAVOR
    const val PROVIDER_RECORD_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/print_logs"
    const val PROVIDER_CONFIG_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/app_configs"
}