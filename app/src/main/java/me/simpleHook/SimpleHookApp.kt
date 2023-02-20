package me.simpleHook

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.SPUtils
import me.simpleHook.util.ThemeModeUtil


lateinit var App: Application
lateinit var sp: SPUtils
class SimpleHookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App = this
        sp = SPUtils(App)
        AppCompatDelegate.setDefaultNightMode(ThemeModeUtil.getDarkMode(sp.themeMode))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(base))
    }
}