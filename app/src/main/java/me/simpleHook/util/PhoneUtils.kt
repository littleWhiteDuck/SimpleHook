package me.simpleHook.util

import android.app.Activity
import android.util.DisplayMetrics

object PhoneUtils {

    @Suppress("unused")
    fun getWindowWidth(activity: Activity): Int {
        return if (OSUtils.atLeastR()) {
            activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels
        }
    }

    fun getAppHeight(activity: Activity): Int {
        return if (OSUtils.atLeastR()) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.heightPixels
        }
    }

}