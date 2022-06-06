package me.simpleHook.hook.extension

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip
import me.simpleHook.hook.getAllTextView

class DialogHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {

    override fun startHook(packageName: String, strConfig: String) {
        val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
        XposedBridge.hookAllMethods(Dialog::class.java, "show", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val dialog = param?.thisObject as Dialog
                val list = mutableListOf<String>()
                val dialogView: View? = dialog.window?.decorView
                dialogView?.also {
                    if (it is ViewGroup) {
                        list += getAllTextView(it)
                    } else if (it is TextView) {
                        list.add(Tip.getTip("text") + it.text.toString())
                    }
                }
                if (configBean.diaCancel) {
                    dialog.setCancelable(true)
                }
                if (configBean.stopDialog.enable) {
                    val showText = list.toString()
                    val keyWords = configBean.stopDialog.info.split("\n")
                    keyWords.forEach {
                        if (it.isNotEmpty() && showText.contains(it)) {
                            dialog.dismiss()
                            val type = if (isShowEnglish) "Dialog(blocked display)" else "弹窗（已拦截）"
                            val log = Gson().toJson(
                                LogBean(
                                    type, list + LogHook.getStackTrace(), packageName
                                )
                            )
                            LogHook.toLogMsg(mContext, log, packageName, type)
                            return
                        }
                    }
                }
                if (configBean.dialog) {
                    val type = if (isShowEnglish) "Dialog" else "弹窗"
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.getStackTrace(), packageName
                        )
                    )
                    LogHook.toLogMsg(mContext, log, packageName, type)
                }
            }
        })
    }
}