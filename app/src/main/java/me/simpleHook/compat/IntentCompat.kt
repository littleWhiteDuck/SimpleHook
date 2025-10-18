package me.simpleHook.compat

import android.content.Intent
import android.os.Parcelable
import me.simpleHook.utils.OSUtil

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
    return if (OSUtil.atLeastT()) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name)
    }
}