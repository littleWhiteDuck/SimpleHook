package me.simpleHook.hook.extension

import android.app.Dialog
import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import kotlinx.serialization.json.Json
import me.simpleHook.data.DialogCancel
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordDialogType
import me.simpleHook.hook.utils.HookUtils
import me.simpleHook.hook.utils.HookUtils.getAllViewIds
import me.simpleHook.hook.utils.RecordOutHelper

object DialogHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {

        if (extensionConfig.stopDialog.enable) {
            findAllMethods(Dialog::class.java) {
                name == "setOnCancelListener" || name == "setOnDismissListener" || name == "setOnShowListener"
            }.hookReturnConstant(null)
        }
        if (extensionConfig.dialog || extensionConfig.diaCancel || extensionConfig.stopDialog.enable) {
            findMethod(Dialog::class.java) {
                name == "show"
            }.hookAfter { param ->
                val dialog = param.thisObject as Dialog
                val textList = mutableListOf<String>()
                val dialogView: View? = dialog.window?.decorView
                dialogView?.also {
                    textList.addAll(HookUtils.getViewAllText(it))
                }
                if (extensionConfig.diaCancel) {
                    dialog.setCancelable(true)
                }
                if (extensionConfig.stopDialog.enable) {
                    val info = extensionConfig.stopDialog.info
                    // new config, not perform old config

                    val dialogCancel = Json.decodeFromString<DialogCancel>(info)
                    if (dialogCancel.keywordEnable) {
                        val showText = textList.toString()
                        val keyWords =
                            Json.decodeFromString<Array<String>>(dialogCancel.keywords)
                        keyWords.forEach {
                            if (it.isNotEmpty() && showText.contains(it)) {
                                dialog.dismiss()
                                RecordOutHelper.outputDialog(type = RecordDialogType.BlockKeyword, textList = textList)
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
                                RecordOutHelper.outputDialog(type = RecordDialogType.BlockId, textList = textList)
                                return@hookAfter
                            }
                        }
                    }

                    if (extensionConfig.dialog) {
                        RecordOutHelper.outputDialog(type = RecordDialogType.Record, textList = textList)
                    }
                }
            }
        }
    }
}