package me.simpleHook

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.SPUtils
import me.simpleHook.util.ThemeModeUtil


lateinit var App: Application

class SimpleHookApp : Application() {
    private val sp by lazy { SPUtils(applicationContext) }
    override fun onCreate() {
        super.onCreate()
        App = this
        AppCompatDelegate.setDefaultNightMode(ThemeModeUtil.getDarkMode(sp.themeMode))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(base))
    }
}