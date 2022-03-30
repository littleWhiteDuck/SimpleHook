package me.simpleHook.util

import android.content.Context
import android.graphics.drawable.Drawable
import me.simpleHook.constant.Constant
import me.simpleHook.ui.custom.CircleTextDrawable

/*
 it
 */
object IconHelper {

    private val iconMap = HashMap<String, Drawable>()

    fun getTextIcon(size: Float = 40f.dp, text: String): Drawable {
        return iconMap[text] ?: CircleTextDrawable(size = size, text = text).also {
            iconMap[text] = it
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable {
        if (packageName == Constant.SIMPLE_HOOK_ERROR) return getTextIcon(text = "Error")
        return iconMap[packageName] ?: AppUtils.getIcon(context, packageName).also {
            iconMap[packageName] = it
        }
    }

    fun isExists(packageName: String): Boolean {
        return iconMap[packageName] != null
    }
}