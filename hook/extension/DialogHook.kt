package me.simpleHook.hook.extension

import android.app.Dialog
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.DialogCancel
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils.getAllTextView
import me.simpleHook.hook.util.HookUtils.getAllViewIds
import me.simpleHook.hook.util.LogUtil

object DialogHook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {

        if (configBean.stopDialog.enable) {
            findAllMethods(Dialog::class.java) {
                name == "setOnCancelListener" || name == "setOnDismissListener" || name == "setOnShowListener"
            }.hookReturnConstant(null)
        }
        if (configBean.dialog || configBean.diaCancel || configBean.stopDialog.enable) {
            findMethod(Dialog::class.java) {
                name == "show"
            }.hookAfter { param ->
                val dialog = param.thisObject as Dialog
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
                    val info = configBean.stopDialog.info
                    // new config, not perform old config
                    if (info[0] == '{' && info[info.length - 1] == '}') {
                        val dialogCancel = Json.decodeFromString<DialogCancel>(info)
                        if (dialogCancel.keywordEnable) {
                            val showText = list.toString()
                            val keyWords =
                                Json.decodeFromString<Array<String>>(dialogCancel.keywords)
                            keyWords.forEach {
                                if (it.isNotEmpty() && showText.contains(it)) {
                                    dialog.dismiss()
                                    val type =
                                        if (isShowEnglish) "Dialog(blocked by keyword)" else "弹窗（通过关键词已拦截）"
                                    LogUtil.outLogMsg(LogBean(type,
                                        list + LogUtil.getStackTrace(),
                                        HookHelper.hostPackageName))
                                    return@hookAfter
                                }
                            }
                        }
                        if (dialogCancel.idEnable) {
                            dialogView ?: return@hookAfter
                            val currentIds = getAllViewIds(dialogView)
                            val ids = Json.decodeFromString<Array<String>>(dialogCancel.ids)
                            currentIds.forEach {
                                if (it in ids) {
                                    dialog.dismiss()
                                    val type =
                                        if (isShowEnglish) "Dialog(blocked by ID)" else "弹窗（通过ID已拦截）"
                                    LogUtil.outLogMsg(LogBean(type,
                                        list + LogUtil.getStackTrace(),
                                        HookHelper.hostPackageName))
                                    return@hookAfter
                                }
                            }
                        }
                    }
                    if (configBean.dialog) {
                        val type = if (isShowEnglish) "Dialog" else "弹窗"
                        LogUtil.outLogMsg(LogBean(type,
                            list + LogUtil.getStackTrace(),
                            HookHelper.hostPackageName))
                    }
                }
            }
        }
    }
}