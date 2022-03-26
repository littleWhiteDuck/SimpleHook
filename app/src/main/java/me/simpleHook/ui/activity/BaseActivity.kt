package me.simpleHook.ui.activity

import android.content.Context
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import me.simpleHook.util.LanguageUtils
import java.lang.reflect.Method

open class BaseActivity : AppCompatActivity() {

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        if (menu.javaClass.simpleName.equals("MenuBuilder", true)){
            try {
                val method: Method =
                    menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.java)
                method.isAccessible = true
                method.invoke(menu, true)
            } catch (e: Exception) {

            }
        }
        return super.onMenuOpened(featureId, menu)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase))
    }
}