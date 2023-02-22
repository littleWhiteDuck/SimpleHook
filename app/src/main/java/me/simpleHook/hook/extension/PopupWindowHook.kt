package me.simpleHook.hook.extension

import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils.getAllTextView
import me.simpleHook.hook.util.LogUtil

object PopupWindowHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.popup || configBean.popCancel || configBean.stopDialog.enable) {
            XposedBridge.hookAllMethods(PopupWindow::class.java,
                "showAtLocation",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        hookPopupWindowDetail(param, configBean)
                    }
                })
            XposedBridge.hookAllMethods(PopupWindow::class.java, "showAsDropDown",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        hookPopupWindowDetail(param, configBean)
                    }
                })
        }
    }

    private fun hookPopupWindowDetail(
        param: XC_MethodHook.MethodHookParam?, configBean: ExtensionConfig
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
                    LogUtil.outLogMsg(LogBean(type,
                        list + LogUtil.getStackTrace(),
                        HookHelper.hostPackageName))
                    param.result = null
                    return@forEach
                }
            }
        }
        if (configBean.popup) {
            val type = "PopupWindow"
            LogUtil.outLogMsg(LogBean(type,
                list + LogUtil.getStackTrace(),
                HookHelper.hostPackageName))
        }
    }

}