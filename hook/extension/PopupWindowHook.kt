package me.simpleHook.hook.extension

import android.content.Context
import android.view.ViewGroup
import android.widget.PopupWindow
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

class PopupWindowHook(mClassLoader: ClassLoader, mContext: Context) :
    BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        XposedBridge.hookAllMethods(PopupWindow::class.java,
            "showAtLocation",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    hookPopupWindowDetail(
                        param, packageName, strConfig
                    )
                }
            })
        XposedBridge.hookAllMethods(PopupWindow::class.java,
            "showAsDropDown",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    hookPopupWindowDetail(
                        param, packageName, strConfig
                    )
                }
            })
    }

    private fun hookPopupWindowDetail(
        param: XC_MethodHook.MethodHookParam?,
        packageName: String,
        strConfig: String
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
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

        val stackTrace = Throwable().stackTrace

        if (configBean.stopDialog.enable) {
            val showText = list.toString()
            val keyWords = configBean.stopDialog.info.split("\n")
            keyWords.forEach {
                if (it.isNotEmpty() && showText.contains(it)) {
                    val type =
                        if (isShowEnglish) "PopupWindow(blocked display)" else "PopupWindow（已拦截）"
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.toStackTrace(stackTrace), packageName
                        )
                    )
                    LogHook.toLogMsg(mContext, log, packageName, type)
                    param.result = null
                    return
                }
            }
        }
        if (configBean.popup) {
            val type = "PopupWindow"
            val log = Gson().toJson(LogBean(type, list + LogHook.toStackTrace(stackTrace), packageName))
            LogHook.toLogMsg(mContext, log, packageName, type)
        }
    }

}