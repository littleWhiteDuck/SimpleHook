package me.simpleHook.platform.hook.utils

import android.app.Application
import android.os.Build
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import java.io.File

object HookHelper {

    fun initFields(context: Context, packageName: String, applicationName: String) {
        appContext = context
        appClassLoader = context.classLoader
        EzXHelperInit.setEzClassLoader(appClassLoader)
        hostPackageName = packageName
        hostProcessName = resolveProcessName(packageName)
        this.applicationName = applicationName
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

    lateinit var hostProcessName: String
        private set


    var enableRecord: Boolean = true

    private fun resolveProcessName(packageName: String): String {
        val processName = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                runCatching { Application.getProcessName() }.getOrNull()
            }

            else -> null
        } ?: runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            activityThreadClass.getDeclaredMethod("currentProcessName").invoke(null) as? String
        }.getOrNull() ?: runCatching {
            val bytes = File("/proc/self/cmdline").readBytes()
            val endIndex = bytes.indexOfFirst { it == 0.toByte() }.takeIf { it >= 0 } ?: bytes.size
            String(bytes, 0, endIndex, Charsets.UTF_8).trim()
        }.getOrNull()

        return processName?.takeIf { it.isNotBlank() } ?: packageName
    }

}
