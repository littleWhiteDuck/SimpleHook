package me.simpleHook

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.SPUtils
import me.simpleHook.util.ThemeModeUtil

class App : Application() {
    private val sp by lazy { SPUtils(applicationContext) }
    override fun onCreate() {
        super.onCreate()
        app = this
        AppCompatDelegate.setDefaultNightMode(ThemeModeUtil.getDarkMode(sp.themeMode))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(base))
    }

    companion object {
        lateinit var app: Application
    }
}