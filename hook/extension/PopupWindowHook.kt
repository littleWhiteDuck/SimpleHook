package me.simpleHook.hook.extension

import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import de.robv.android.xposed.XC_MethodHook
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.DialogCancel
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.HookUtils
import me.simpleHook.hook.util.HookUtils.getAllTextView
import me.simpleHook.hook.util.LogUtil

object PopupWindowHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.popup || configBean.popCancel || configBean.stopDialog.enable) {
            findMethod(PopupWindow::class.java) {
                name == "showAtLocation" && parameterTypes[0].isInterface
            }.hookBefore {
                hookPopupWindowDetail(it, configBean)
            }
            findMethod(PopupWindow::class.java) {
                name == "showAsDropDown" && paramCount == 4
            }.hookBefore {
                hookPopupWindowDetail(it, configBean)
            }
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
            val info = configBean.stopDialog.info
            // new config, not perform old config
            if (info[0] == '{' && info[info.length - 1] == '}') {
                val dialogCancel = Json.decodeFromString<DialogCancel>(info)
                if (dialogCancel.keywordEnable) {
                    val showText = list.toString()
                    val keyWords = Json.decodeFromString<Array<String>>(dialogCancel.keywords)
                    keyWords.forEach {
                        if (it.isNotEmpty() && showText.contains(it)) {
                            param.result = null
                            val type =
                                if (isShowEnglish) "PopupWindow(blocked by keyword)" else "PopupWindow（通过关键词已拦截）"
                            LogUtil.outLogMsg(LogBean(type,
                                list + LogUtil.getStackTrace(),
                                HookHelper.hostPackageName))
                            return
                        }
                    }
                }
                if (dialogCancel.idEnable) {
                    val currentIds = HookUtils.getAllViewIds(contentView)
                    val ids = Json.decodeFromString<Array<String>>(dialogCancel.ids)
                    currentIds.forEach {
                        if (it in ids) {
                            param.result = null
                            val type =
                                if (isShowEnglish) "PopupWindow(blocked by ID)" else "PopupWindow（通过ID已拦截）"
                            LogUtil.outLogMsg(LogBean(type,
                                list + LogUtil.getStackTrace(),
                                HookHelper.hostPackageName))
                            return
                        }
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
}