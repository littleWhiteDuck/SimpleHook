package me.simpleHook.hook.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit

object HookHelper {

    fun initFields(context: Context, packageName: String, app: ApplicationInfo) {
        appContext = context
        appClassLoader = context.classLoader
        EzXHelperInit.setEzClassLoader(appClassLoader)
        hostPackageName = packageName
        appInfo = app
    }

    lateinit var appContext: Context
        private set

    val isAppContextInitialized: Boolean
        get() = this::appContext.isInitialized

    lateinit var appClassLoader: ClassLoader
        private set


    lateinit var hostPackageName: String
        private set

    lateinit var appInfo: ApplicationInfo

    var enableRecord: Boolean = true


}