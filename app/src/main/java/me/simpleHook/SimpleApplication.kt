package me.simpleHook

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.SPUtils

class SimpleApplication : Application() {
    private val sp by lazy { SPUtils(applicationContext) }
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(sp.uiMode)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(base))
    }
}