package me.simpleHook.platform.hook.extension

import android.app.Dialog
import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordDialogType
import me.simpleHook.platform.hook.utils.HookUtils
import me.simpleHook.platform.hook.utils.RecordOutHelper

object DialogHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        val dialogConfig = extensionConfig.popupConfig

        if (dialogConfig.cancelDialog) {
            findAllMethods(Dialog::class.java) {
                name == "setOnCancelListener" || name == "setOnDismissListener" || name == "setOnShowListener"
            }.hookReturnConstant(null)
        }

        if (dialogConfig.recordDialog || dialogConfig.cancelDialog || dialogConfig.blockDialog.enable) {
            findMethod(Dialog::class.java) {
                name == "show"
            }.hookAfter { param ->
                val dialog = param.thisObject as Dialog
                val textList = mutableListOf<String>()
                val dialogView: View? = dialog.window?.decorView
                dialogView?.also {
                    textList.addAll(HookUtils.collectViewTexts(it))
                }
                if (dialogConfig.cancelDialog) {
                    dialog.setCancelable(true)
                }
                val blockDialog = dialogConfig.blockDialog

                if (blockDialog.enable) {
                    if (blockDialog.keywordEnable) {
                        val showText = textList.toString()
                        blockDialog.keywords.forEach {
                            if (it.isNotEmpty() && showText.contains(it)) {
                                dialog.dismiss()
                                RecordOutHelper.outputDialog(
                                    type = RecordDialogType.BlockKeyword,
                                    textList = textList
                                )
                                return@hookAfter
                            }
                        }
                    }
                    if (blockDialog.idEnable) {
                        dialogView ?: return@hookAfter
                        val currentIds = HookUtils.collectViewIds(dialogView)
                        currentIds.forEach {
                            if (it in blockDialog.ids) {
                                dialog.dismiss()
                                RecordOutHelper.outputDialog(
                                    type = RecordDialogType.BlockId,
                                    textList = textList
                                )
                                return@hookAfter
                            }
                        }
                    }
                }
                if (dialogConfig.recordDialog) {
                    RecordOutHelper.outputDialog(
                        type = RecordDialogType.Record,
                        textList = textList
                    )
                }
            }
        }
    }
}