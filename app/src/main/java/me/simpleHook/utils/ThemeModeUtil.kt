package me.simpleHook.utils

import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode

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

    fun isDarkMode(): Boolean {
        return when (getDefaultNightMode()) {
            MODE_NIGHT_YES -> true
            MODE_NIGHT_NO -> false
            MODE_NIGHT_FOLLOW_SYSTEM, MODE_NIGHT_AUTO_BATTERY, MODE_NIGHT_UNSPECIFIED -> isDarkModeOnSystem()
            else -> false
        }
    }

    fun isDarkModeOnSystem(): Boolean {
        return when (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }
}