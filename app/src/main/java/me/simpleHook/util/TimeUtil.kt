package me.simpleHook.util

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Build

object TimeUtil {
    /**
     * 获取最后一次更新时间
     */
    @SuppressLint("SimpleDateFormat")
    fun getDateTime(time: Long, pattern: String = "yy-MM-dd") =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat(pattern).format(time)
        } else ""
}