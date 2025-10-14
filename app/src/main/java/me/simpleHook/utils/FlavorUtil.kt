package me.simpleHook.utils

import me.simpleHook.BuildConfig

object FlavorUtil {

    const val liteVersion = BuildConfig.FLAVOR == "lite"
    const val rootVersion = BuildConfig.FLAVOR == "root"
    const val normalVersion = BuildConfig.FLAVOR == "normal"
    const val AUTHORITIES = "me.simplehook.provider." + BuildConfig.FLAVOR
    const val PROVIDER_RECORD_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/print_logs"
    const val PROVIDER_CUSTOM_CONFIG_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/app_configs"
    const val PROVIDER_EXTENSION_CONFIG_URI =
        "content://me.simplehook.provider." + BuildConfig.FLAVOR + "/assist_configs"
    const val betaVersion = BuildConfig.IS_BETA
}