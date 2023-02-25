package me.simpleHook

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.ThemeModeUtil


lateinit var App: Application

class SimpleHookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App = this
        AppCompatDelegate.setDefaultNightMode(ThemeModeUtil.getDarkMode(GlobalValue.sp.themeMode))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(base))
    }
}