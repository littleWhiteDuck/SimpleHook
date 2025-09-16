package me.simpleHook.hook.extension

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.LogBean
import me.simpleHook.hook.language.tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils.getAllTextView
import me.simpleHook.hook.util.LogUtil
import me.simpleHook.hook.util.log

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
                        list.add(tip.text + it)
                    }
                } catch (_: NoSuchFieldError) {
                    "toast error1".log()
                    try {
                        XposedHelpers.getObjectField(toast, "mNextView")?.also {
                            val toastView = it as View
                            if (toastView is ViewGroup) {
                                list += getAllTextView(toastView)
                            } else if (toastView is TextView) {
                                list.add(tip.text + toastView.text.toString())
                            }
                        }
                    } catch (_: NoSuchFieldError) {
                        "toast error2".log()
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