package me.simpleHook.util

import androidx.appcompat.app.AppCompatDelegate.*

object ThemeModeUtil {

    fun setMode(value: String) {
        setDefaultNightMode(getDarkMode(value))
    }

    fun getDarkMode(value: String?): Int {
        return when (value) {
            "light" -> MODE_NIGHT_NO
            "dark" -> MODE_NIGHT_YES
            else -> MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}