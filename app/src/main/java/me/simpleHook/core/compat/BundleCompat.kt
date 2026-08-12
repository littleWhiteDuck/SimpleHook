package me.simpleHook.core.compat

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import me.simpleHook.core.utils.OSUtil


@Suppress("DEPRECATION", "unused")
object BundleCompat {

    inline fun <reified T : Parcelable> getParcelable(bundle: Bundle, key: String?): T? {
        return if (OSUtil.atLeastT()) {
            BundleCompat.getParcelable(bundle, key, T::class.java)
        } else {
            bundle.getParcelable(key)
        }
    }
}