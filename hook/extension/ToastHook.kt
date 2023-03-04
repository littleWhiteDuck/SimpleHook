package me.simpleHook.hook.extension

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils.getAllTextView
import me.simpleHook.hook.util.LogUtil
import me.simpleHook.extension.log

object ToastHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.toast) return
        XposedHelpers.findAndHookMethod(Toast::class.java, "show", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val list = mutableListOf<String>()
                // not test some cases
                val toast: Toast = param?.thisObject as Toast
                try {
                    XposedHelpers.getObjectField(toast, "mText")?.also {
                        list.add(Tip.getTip("text") + it)
                    }
                } catch (e: NoSuchFieldError) {
                    "toast error1".log(HookHelper.hostPackageName)
                    try {
                        XposedHelpers.getObjectField(toast, "mNextView")?.also {
                            val toastView = it as View
                            if (toastView is ViewGroup) {
                                list += getAllTextView(toastView)
                            } else if (toastView is TextView) {
                                list.add(Tip.getTip("text") + toastView.text.toString())
                            }
                        }
                    } catch (e: NoSuchFieldError) {
                        "toast error2".log(HookHelper.hostPackageName)
                    }
                }
                val type = "Toast"
                val logBean =
                    LogBean(type, list + LogUtil.getStackTrace(), HookHelper.hostPackageName)
                LogUtil.outLogMsg(logBean)
            }
        })
    }
}