package me.simpleHook.util

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager

object PhoneUtils {
    fun getWindowWidth(context: Context): Int {
        return (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.width
    }

    fun getAppHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getSize(point)
        return point.y
    }
    fun getViewY(view: View): Int{
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[1]
    }

}