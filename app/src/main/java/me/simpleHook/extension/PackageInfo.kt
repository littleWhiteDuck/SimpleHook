package me.simpleHook.extension

import android.content.pm.PackageInfo
import me.simpleHook.utils.OSUtil

val PackageInfo.verCode
    get() = run {
        if (OSUtil.atLeastP()) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode
        }
    }