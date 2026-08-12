package me.simpleHook.core.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics


object WindowUtil {

    @Suppress("unused", "DEPRECATION")
    fun getWindowWidth(activity: Activity): Int {
        return if (OSUtil.atLeastR()) {
            activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels
        }
    }

    @Suppress("DEPRECATION")
    fun getAppHeight(activity: Activity): Int {
        return if (OSUtil.atLeastR()) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
//            val rootWindowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            // not include system bar height
            metrics.heightPixels
        }
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

}