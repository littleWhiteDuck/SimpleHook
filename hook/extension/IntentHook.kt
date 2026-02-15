package me.simpleHook.platform.hook.extension

import android.content.Intent
import android.os.Bundle
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper

private const val ACTIVITY = "android.app.Activity"
private const val CONTEXT_WRAPPER = "android.content.ContextWrapper"
private const val START_ACTIVITY = "startActivity"
private const val START_ACTIVITY_FOR_RESULT = "startActivityForResult"

object IntentHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.intent) return
        XposedHelpers.findAndHookMethod(
            ACTIVITY,
            HookHelper.appClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    RecordOutHelper.outputIntent(intent)
                }
            })

        XposedHelpers.findAndHookMethod(
            CONTEXT_WRAPPER,
            HookHelper.appClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    RecordOutHelper.outputIntent(intent)

                }
            })

        XposedHelpers.findAndHookMethod(
            CONTEXT_WRAPPER,
            HookHelper.appClassLoader,
            START_ACTIVITY,
            Intent::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    RecordOutHelper.outputIntent(intent)

                }
            })

        XposedHelpers.findAndHookMethod(
            ACTIVITY,
            HookHelper.appClassLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    RecordOutHelper.outputIntent(intent)

                }
            })
        XposedHelpers.findAndHookMethod(
            ACTIVITY,
            HookHelper.appClassLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    RecordOutHelper.outputIntent(intent)

                }
            })
    }
}