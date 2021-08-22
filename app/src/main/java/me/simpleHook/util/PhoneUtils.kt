package me.simpleHook.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

object PhoneUtils {
    fun getWindowWidth(context: Context): Int {
        return (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.width
    }

    //应用界面可见高度，可能不包含导航和状态栏，看Rom实现
    fun getAppHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            ?: return -1
        val point = Point()
        wm.defaultDisplay.getSize(point)
        return point.y
    }
}