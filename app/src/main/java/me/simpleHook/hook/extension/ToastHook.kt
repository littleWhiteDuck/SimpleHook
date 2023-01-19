package me.simpleHook.hook.extension

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.hook.Tip
import me.simpleHook.hook.utils.getAllTextView
import me.simpleHook.util.log

object ToastHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
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
                    "toast error1".log(packageName)
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
                        "toast error2".log(packageName)
                    }
                }
                val type = "Toast"
                val log = Gson().toJson(
                    LogBean(
                        type, list + LogUtil.getStackTrace(), packageName
                    )
                )
                LogUtil.toLogMsg(log, packageName, type)
            }
        })
    }
}