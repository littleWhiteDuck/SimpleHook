package me.simpleHook.extension

import android.content.pm.PackageInfo
import me.simpleHook.util.OSUtils

val PackageInfo.verCode
    get() = run {
        if (OSUtils.atLeastP()) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode
        }
    }