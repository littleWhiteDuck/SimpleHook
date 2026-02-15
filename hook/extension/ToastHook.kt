package me.simpleHook.platform.hook.extension

import android.view.View
import android.widget.Toast
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.platform.hook.utils.HookUtils
import me.simpleHook.platform.hook.utils.RecordOutHelper

object ToastHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.popupConfig.recordToast) return
        XposedHelpers.findAndHookMethod(Toast::class.java, "show", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val textList = mutableListOf<String>()
                val toast: Toast = param?.thisObject as Toast

                runCatching {
                    XposedHelpers.getObjectField(toast, "mText")?.also {
                        textList.add(it.toString())
                    }
                }

                runCatching {
                    XposedHelpers.getObjectField(toast, "mNextView")?.also {
                        val toastView = it as View
                        textList.addAll(HookUtils.collectViewTexts(toastView))
                    }
                }

                RecordOutHelper.outputToast(textList = textList)
            }
        })
    }
}
