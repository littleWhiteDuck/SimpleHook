package me.simpleHook.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

object TimeUtil {
    /**
     * 获取最后一次更新时间
     */
    @SuppressLint("SimpleDateFormat")
    fun getDateTime(time: Long, pattern: String = "yy-MM-dd"): String =
        SimpleDateFormat(pattern).format(time)
}