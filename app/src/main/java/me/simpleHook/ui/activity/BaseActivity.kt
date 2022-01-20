package me.simpleHook.ui.activity

import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import java.lang.reflect.Method

open class BaseActivity : AppCompatActivity() {

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        if (menu.javaClass.simpleName.equals("MenuBuilder", true)){
            try {
                val method: Method = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.java)
                method.isAccessible = true
                method.invoke(menu, true)
            }catch (e: Exception){

            }
        }
        return super.onMenuOpened(featureId, menu)
    }

}