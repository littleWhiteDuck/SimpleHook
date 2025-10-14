package me.simpleHook.utils

import android.annotation.SuppressLint
import android.content.Context
import me.simpleHook.R
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
            return context.getString(R.string.time_illegal)
        }
        val second = (nowTime - oldTime) / 1000
        if (second <= 60) {
            return context.getString(R.string.time_seconds_ago, second)
        }
        val minute = second / 60
        if (minute == 1L) {
            return context.getString(R.string.time_a_minute_ago)
        }
        if (minute <= 60) {
            return context.getString(R.string.time_minutes_ago, minute)
        }
        val hour = minute / 60
        if (hour == 1L) {
            return context.getString(R.string.time_an_hour_ago)
        }
        if (hour <= 24) {
            return context.getString(R.string.time_hours_ago, hour)
        }
        val day = hour / 24
        if (day == 1L) {
            return context.getString(R.string.time_a_day_ago)
        }
        if (day <= 30) {
            return context.getString(R.string.time_days_ago, day)
        }
        val month = day / 30
        if (month == 1L) {
            return context.getString(R.string.time_a_month_ago)
        }
        if (month <= 12) {
            return context.getString(R.string.time_months_ago, month)
        }
        val year = month / 12
        if (year == 1L) {
            return context.getString(R.string.time_a_year_ago)
        }
        return context.getString(R.string.time_years_ago, year)
    }
}
