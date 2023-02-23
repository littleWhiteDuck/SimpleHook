package me.simpleHook.compat

import android.os.Bundle
import android.os.Parcelable
import me.simpleHook.util.OSUtils
import java.io.Serializable


@Suppress("DEPRECATION", "unused")
object BundleCompat {

    inline fun <reified T : Parcelable> getParcelable(bundle: Bundle, key: String?): T? {
        return if (OSUtils.atLeastT()) {
            bundle.getParcelable(key, T::class.java)
        } else {
            bundle.getParcelable(key)
        }
    }
}