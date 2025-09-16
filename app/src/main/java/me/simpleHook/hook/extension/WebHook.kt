package me.simpleHook.hook.extension

import android.webkit.WebView
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.LogBean
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil

object WebHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.webLoadUrl) {
            hookWebLoadUrl()
        }
        if (configBean.webDebug) {
            hookWebDebug()
        }
    }


    private fun hookWebLoadUrl() {
        XposedBridge.hookAllMethods(WebView::class.java, "loadUrl", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = "WEB"
                val url = param.args[0] as String
                val list = mutableListOf<String>()
                list.add("Url: $url")
                if (param.args.size == 2) {
                    @Suppress("UNCHECKED_CAST")
                    val map = param.args[1] as Map<String, String>
                    val headers = Json.encodeToString(map)
                    list.add("Header: $headers")
                }
                val logBean = LogBean(type, list, HookHelper.hostPackageName)
                LogUtil.outLogMsg(logBean)
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
        XposedHelpers.findAndHookMethod(webClass,
            "setWebContentsDebuggingEnabled",
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = true
                }
            })
    }

}