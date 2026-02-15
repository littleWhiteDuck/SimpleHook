package me.simpleHook.platform.hook.utils

import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit

object HookHelper {

    fun initFields(context: Context, packageName: String, applicationName: String) {
        appContext = context
        appClassLoader = context.classLoader
        EzXHelperInit.setEzClassLoader(appClassLoader)
        hostPackageName = packageName
        this.applicationName = applicationName
        RecordOutHelper.ensureRecordStorageReady()
    }

    lateinit var applicationName: String
        private set

    lateinit var appContext: Context
        private set

    val isAppContextInitialized: Boolean
        get() = this::appContext.isInitialized

    lateinit var appClassLoader: ClassLoader
        private set


    lateinit var hostPackageName: String
        private set


    var enableRecord: Boolean = true


}
