package me.simpleHook.hook.extension

import android.webkit.WebView
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.HookHelper
import me.simpleHook.hook.utils.LogUtil

object WebHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean) {
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
                    val headers = Gson().toJson(param.args[1])
                    list.add("Header: $headers")
                }
                val logBean = LogBean(type, list, HookHelper.hostPackageName)
                LogUtil.toLogMsg(Gson().toJson(logBean), type)
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