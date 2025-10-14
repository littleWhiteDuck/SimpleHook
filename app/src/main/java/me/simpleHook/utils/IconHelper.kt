@file:Suppress("unused")

package me.simpleHook.utils

import android.content.Context
import android.graphics.drawable.Drawable
import me.simpleHook.extension.dp
import me.simpleHook.ui.custom.CircleTextDrawable


object IconHelper {

    private val iconMap = HashMap<String, Drawable>()

    fun getTextIcon(size: Float = 40f.dp, text: String): Drawable {
        return iconMap[text] ?: CircleTextDrawable(size = size, text = text).also {
            iconMap[text] = it
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable {
        if (packageName.startsWith("error")) return getTextIcon(text = "Error")
        return iconMap[packageName] ?: AppUtil.getIcon(packageName).also {
            iconMap[packageName] = it
        }
    }

    fun isExists(packageName: String): Boolean {
        return iconMap[packageName] != null
    }

    fun loadIcon(context: Context, packageName: String) {
        if (iconMap[packageName] == null) {
            AppUtil.getIcon(packageName).also {
                iconMap[packageName] = it
            }
        }
    }
}