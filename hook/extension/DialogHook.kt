package me.simpleHook.hook.extension

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.utils.HookHelper
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.hook.utils.getAllTextView

object DialogHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean) {

        if (configBean.stopDialog.enable) {
            findAllMethods(Dialog::class.java) {
                name == "setOnCancelListener" || name == "setOnDismissListener" || name == "setOnShowListener"
            }.hookReturnConstant(null)
        }
        if (configBean.dialog || configBean.diaCancel || configBean.stopDialog.enable) {
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
                                val type =
                                    if (isShowEnglish) "Dialog(blocked display)" else "弹窗（已拦截）"
                                val log = Gson().toJson(
                                    LogBean(
                                        type,
                                        list + LogUtil.getStackTrace(),
                                        HookHelper.hostPackageName
                                    )
                                )
                                LogUtil.toLogMsg(log, type)
                                return
                            }
                        }
                    }
                    if (configBean.dialog) {
                        val type = if (isShowEnglish) "Dialog" else "弹窗"
                        val log = Gson().toJson(
                            LogBean(
                                type, list + LogUtil.getStackTrace(), HookHelper.hostPackageName
                            )
                        )
                        LogUtil.toLogMsg(log, type)
                    }
                }
            })
        }
    }
}