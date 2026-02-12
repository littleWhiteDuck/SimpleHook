package me.simpleHook.core.extension

import android.content.pm.PackageInfo
import me.simpleHook.core.utils.OSUtil

val PackageInfo.verCode
    get() = run {
        if (OSUtil.atLeastP()) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode
        }
    }