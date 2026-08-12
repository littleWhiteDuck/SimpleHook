package me.simpleHook.platform.hook.extension

import android.webkit.WebView
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordWebLoadUrl
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper

object WebHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.webConfig.recordUrl) {
            hookWebLoadUrl()
        }
        if (extensionConfig.webConfig.enableDebug) {
            hookWebDebug()
        }
    }


    private fun hookWebLoadUrl() {
        XposedBridge.hookAllMethods(WebView::class.java, "loadUrl", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val url = param.args[0] as String
                val headers = if (param.args.size == 2) {
                    @Suppress("UNCHECKED_CAST")
                    param.args[1] as Map<String, String>
                } else {
                    emptyMap()
                }

                RecordOutHelper.outputRecord(
                    type = RecordType.WebLoadUrl, record = RecordWebLoadUrl(
                        url = url,
                        headers = headers
                    )
                )
            }
        })
    }

    private fun hookWebDebug() {
        val webClass = XposedHelpers.findClass("android.webkit.WebView", HookHelper.appClassLoader)
        XposedBridge.hookAllConstructors(webClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedHelpers.callStaticMethod(webClass, "setWebContentsDebuggingEnabled", true)
            }
        })
        XposedHelpers.findAndHookMethod(
            webClass,
            "setWebContentsDebuggingEnabled",
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = true
                }
            })
    }

}