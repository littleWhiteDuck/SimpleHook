package me.simpleHook.hook.extension

import android.webkit.WebView
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil

object WebHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
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
                LogUtil.toLogMsg(Gson().toJson(logBean), packageName, type)
            }
        })
    }

    private fun hookWebDebug(packageName: String) {
        val webClass = XposedHelpers.findClass("android.webkit.WebView", InitFields.ezXClassLoader)
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