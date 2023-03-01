package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.Context
import java.text.SimpleDateFormat

object TimeUtil {

    @SuppressLint("SimpleDateFormat")
    fun getTime(time: Long, pattern: String = "yy-MM-dd"): String =
        SimpleDateFormat(pattern).format(time)

    @SuppressLint("SimpleDateFormat")
    fun getCurrentTime(pattern: String = "yy-MM-dd"): String =
        getTime(System.currentTimeMillis(), pattern)

    fun calculateRangeToNow(context: Context, oldTime: Long): String {
        val nowTime = System.currentTimeMillis()
        if (nowTime - oldTime <= 0) {
            return "你穿越时空了？"
        }
        val second = (nowTime - oldTime) / 1000
        if (second <= 60) {
            return "${second}秒前"
        }
        val minute = second / 60
        if (minute <= 60) {
            return "${minute}分钟前"
        }
        val hour = minute / 60
        if (hour <= 24) {
            return "${hour}小时前"
        }
        val day = hour / 24
        if (day <= 30) {
            return "${day}天前"
        }
        val month = day / 30
        if (month <= 12) {
            return "${month}月前"
        }
        return "${month / 12}年前"
    }
}
