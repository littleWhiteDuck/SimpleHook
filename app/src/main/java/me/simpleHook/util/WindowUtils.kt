package me.simpleHook.util

import android.app.Activity
import android.util.DisplayMetrics
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
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.heightPixels + WindowInsetsCompat.Type.systemBars()
        }
    }

}