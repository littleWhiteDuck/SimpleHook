package me.simpleHook.feature.dexbrowser.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import me.simpleHook.core.GlobalValue
import me.simpleHook.feature.dexbrowser.ui.compose.page.DexBrowser
import me.simpleHook.feature.dexbrowser.ui.compose.theme.SimpleHookTheme
import me.simpleHook.core.utils.LanguageUtil

class DexBrowserActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleHookTheme(
                dynamicColor = GlobalValue.sp.enableSystemAccent,
                darkTheme = isDarkTheme()
            ) {
                DexBrowser(onBack = { finish() }, onFieldDone = { className, info ->
                    val intent = Intent().apply {
                        putExtra("className", className)
                        putExtra("fieldInfo", info)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }, onMethodDone = { className, info ->
                    val intent = Intent().apply {
                        putExtra("className", className)
                        putExtra("methodInfo", info)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                })
            }
        }
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return when (GlobalValue.sp.themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }
    }
}


