package me.simpleHook.hook.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import de.robv.android.xposed.callbacks.XC_LoadPackage

object HookHelper {

    fun initFields(context: Context, lpparam: XC_LoadPackage.LoadPackageParam) {
        appContext = context
        appClassLoader = context.classLoader
        hostPackageName = lpparam.packageName
        appInfo = lpparam.appInfo
    }

    lateinit var appContext: Context
        internal set

    val isAppContextInitialized: Boolean
        get() = this::appContext.isInitialized

    lateinit var appClassLoader: ClassLoader
        internal set


    lateinit var hostPackageName: String
        internal set

    lateinit var appInfo: ApplicationInfo


}