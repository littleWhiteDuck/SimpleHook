package me.simpleHook.hook.extension

import android.content.Context
import android.webkit.WebView
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook

class WebHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
        if (configBean.webLoadUrl) {
            hookWebLoadUrl(packageName)
        }
        if (configBean.webDebug) {
            hookWebDebug(packageName)
        }
    }


    private fun hookWebLoadUrl(packageName: String) {
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
                val logBean = LogBean(type, list, packageName)
                LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
            }
        })
    }

    private fun hookWebDebug(packageName: String) {
        val webClass = XposedHelpers.findClass("android.webkit.WebView", mClassLoader)
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