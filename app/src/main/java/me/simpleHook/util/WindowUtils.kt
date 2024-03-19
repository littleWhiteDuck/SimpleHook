package me.simpleHook.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


object WindowUtils {

    @Suppress("unused", "DEPRECATION")
    fun getWindowWidth(activity: Activity): Int {
        return if (OSUtils.atLeastR()) {
            activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels
        }
    }

    @Suppress("DEPRECATION")
    fun getAppHeight(activity: Activity): Int {
        return if (OSUtils.atLeastR()) {
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