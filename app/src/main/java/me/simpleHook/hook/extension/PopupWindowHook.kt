package me.simpleHook.hook.extension

import android.widget.PopupWindow
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.util.xpcompat.XC_MethodHook
import kotlinx.serialization.json.Json
import me.simpleHook.data.DialogCancel
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordPopupWindowType
import me.simpleHook.hook.utils.HookUtils
import me.simpleHook.hook.utils.RecordOutHelper

object PopupWindowHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.popup || extensionConfig.popCancel || extensionConfig.stopDialog.enable) {
            findMethod(PopupWindow::class.java) {
                name == "showAtLocation" && parameterTypes[0].isInterface
            }.hookBefore {
                hookPopupWindowDetail(it, extensionConfig)
            }
            findMethod(PopupWindow::class.java) {
                name == "showAsDropDown" && paramCount == 4
            }.hookBefore {
                hookPopupWindowDetail(it, extensionConfig)
            }
        }
    }

    private fun hookPopupWindowDetail(
        param: XC_MethodHook.MethodHookParam?, extensionConfig: ExtensionConfig
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        if (extensionConfig.popCancel) {
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
        }
        val textList = mutableListOf<String>()
        val contentView = popupWindow.contentView
        textList.addAll(HookUtils.getViewAllText(contentView))

        if (extensionConfig.stopDialog.enable) {
            val info = extensionConfig.stopDialog.info
            val dialogCancel = Json.decodeFromString<DialogCancel>(info)
            if (dialogCancel.keywordEnable) {
                val showText = textList.toString()
                val keyWords = Json.decodeFromString<Array<String>>(dialogCancel.keywords)
                keyWords.forEach {
                    if (it.isNotEmpty() && showText.contains(it)) {
                        param.result = null
                        RecordOutHelper.outputPopup(
                            type = RecordPopupWindowType.BlockKeyword,
                            textList = textList
                        )
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
                        RecordOutHelper.outputPopup(
                            type = RecordPopupWindowType.BlockId,
                            textList = textList
                        )
                        return
                    }
                }
            }
            if (extensionConfig.popup) {
                RecordOutHelper.outputPopup(
                    type = RecordPopupWindowType.Record,
                    textList = textList
                )
            }
        }
    }
}