package me.simpleHook.platform.hook.extension

import android.widget.PopupWindow
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.util.xpcompat.XC_MethodHook
import me.simpleHook.data.ExPopupConfig
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordPopupWindowType
import me.simpleHook.platform.hook.utils.HookUtils
import me.simpleHook.platform.hook.utils.RecordOutHelper

object PopupWindowHook : BaseHook() {
    override fun startHook(extensionConfig: ExtensionConfig) {
        val dialogConfig = extensionConfig.popupConfig

        if (dialogConfig.recordPopup || dialogConfig.cancelPopup || dialogConfig.blockDialog.enable) {
            findMethod(PopupWindow::class.java) {
                name == "showAtLocation" && parameterTypes[0].isInterface
            }.hookBefore {
                hookPopupWindowDetail(it, dialogConfig)
            }
            findMethod(PopupWindow::class.java) {
                name == "showAsDropDown" && paramCount == 4
            }.hookBefore {
                hookPopupWindowDetail(it, dialogConfig)
            }
        }
    }

    private fun hookPopupWindowDetail(
        param: XC_MethodHook.MethodHookParam?, dialogConfig: ExPopupConfig
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        if (dialogConfig.cancelPopup) {
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
        }
        val textList = mutableListOf<String>()
        val contentView = popupWindow.contentView
        textList.addAll(HookUtils.collectViewTexts(contentView))

        val blockDialog = dialogConfig.blockDialog
        if (blockDialog.enable) {
            if (blockDialog.keywordEnable) {
                val showText = textList.toString()
                blockDialog.keywords.forEach {
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
            if (blockDialog.idEnable) {
                val currentIds = HookUtils.collectViewIds(contentView)
                currentIds.forEach {
                    if (it in blockDialog.ids) {
                        param.result = null
                        RecordOutHelper.outputPopup(
                            type = RecordPopupWindowType.BlockId,
                            textList = textList
                        )
                        return
                    }
                }
            }
        }
        if (dialogConfig.recordPopup) {
            RecordOutHelper.outputPopup(
                type = RecordPopupWindowType.Record,
                textList = textList
            )
        }
    }
}