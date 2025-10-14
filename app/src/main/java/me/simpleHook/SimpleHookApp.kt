package me.simpleHook

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import me.simpleHook.shizuku.ShizukuFileManager
import me.simpleHook.utils.LanguageUtil
import me.simpleHook.utils.ThemeModeUtil


lateinit var App: Application

class SimpleHookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App = this
        if (DynamicColors.isDynamicColorAvailable() && GlobalValue.sp.enableSystemAccent) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        AppCompatDelegate.setDefaultNightMode(ThemeModeUtil.getDarkMode(GlobalValue.sp.themeMode))
        ShizukuFileManager.init()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(base))
    }
}