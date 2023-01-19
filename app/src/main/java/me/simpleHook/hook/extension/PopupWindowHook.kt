package me.simpleHook.hook.extension

import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.hook.Tip
import me.simpleHook.hook.utils.getAllTextView

object PopupWindowHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
        if (configBean.popup || configBean.popCancel || configBean.stopDialog.enable) {
            XposedBridge.hookAllMethods(
                PopupWindow::class.java,
                "showAtLocation",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        hookPopupWindowDetail(
                            param, packageName, configBean
                        )
                    }
                })
            XposedBridge.hookAllMethods(
                PopupWindow::class.java,
                "showAsDropDown",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        hookPopupWindowDetail(
                            param, packageName, configBean
                        )
                    }
                })
        }
    }

    private fun hookPopupWindowDetail(
        param: XC_MethodHook.MethodHookParam?, packageName: String, configBean: ExtensionConfigBean
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        if (configBean.popCancel) {
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
        }
        val list = mutableListOf<String>()
        val contentView = popupWindow.contentView
        if (contentView is ViewGroup) {
            list += getAllTextView(contentView)
        } else if (contentView is TextView) {
            list.add(Tip.getTip("text") + contentView.text.toString())
        }
        if (configBean.stopDialog.enable) {
            val showText = list.toString()
            val keyWords = configBean.stopDialog.info.split("\n")
            keyWords.forEach {
                if (it.isNotEmpty() && showText.contains(it)) {
                    val type =
                        if (isShowEnglish) "PopupWindow(blocked display)" else "PopupWindow（已拦截）"
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogUtil.getStackTrace(), packageName
                        )
                    )
                    LogUtil.toLogMsg(log, packageName, type)
                    param.result = null
                    return
                }
            }
        }
        if (configBean.popup) {
            val type = "PopupWindow"
            val log = Gson().toJson(LogBean(type, list + LogUtil.getStackTrace(), packageName))
            LogUtil.toLogMsg(log, packageName, type)
        }
    }

}