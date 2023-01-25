package me.simpleHook.hook.extension

import android.util.Base64
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip
import me.simpleHook.hook.utils.HookHelper
import me.simpleHook.hook.utils.LogUtil
import java.nio.charset.Charset

object Base64Hook : BaseHook() {

    override fun startHook(configBean: ExtensionConfigBean) {
        if (!configBean.base64) return
        XposedHelpers.findAndHookMethod("java.util.Base64.Encoder",
            HookHelper.appClassLoader,
            "encode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val items = LogUtil.getStackTrace()
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isEncrypt"),
                            Tip.getTip("rawData") + String(data),
                            Tip.getTip("encryptResult") + result
                        ) + items, HookHelper.hostPackageName
                    )
                    LogUtil.toLogMsg(
                        Gson().toJson(logBean), logBean.type
                    )
                }
            })

        XposedHelpers.findAndHookMethod("java.util.Base64.Decoder",
            HookHelper.appClassLoader,
            "decode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val items = LogUtil.getStackTrace().toList()
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isDecrypt"),
                            Tip.getTip("rawData") + String(data),
                            Tip.getTip("decryptResult") + result
                        ) + items, HookHelper.hostPackageName
                    )
                    LogUtil.toLogMsg(
                        Gson().toJson(logBean), logBean.type
                    )
                }
            })

        XposedHelpers.findAndHookMethod(Base64::class.java,
            "encode",
            ByteArray::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    /*
                    byte[] encode(byte[] input, int flags)
                    byte[] encode(byte[] input, int offset, int len, int flags)
                     */
                    val input = param.args[0] as ByteArray
                    val offset = param.args[1] as Int
                    val len = param.args[2] as Int
                    val rawData = ByteArray(len)
                    System.arraycopy(input, offset, rawData, 0, len)
                    val items = LogUtil.getStackTrace()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isEncrypt"),
                            Tip.getTip("rawData") + String(rawData),
                            Tip.getTip("encryptResult") + result
                        ) + items, HookHelper.hostPackageName
                    )
                    LogUtil.toLogMsg(Gson().toJson(logBean), logBean.type)
                }
            })

        XposedHelpers.findAndHookMethod(Base64::class.java,
            "decode",
            ByteArray::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val input = param.args[0] as ByteArray
                    val offset = param.args[1] as Int
                    val len = param.args[2] as Int
                    val rawData = ByteArray(len)
                    System.arraycopy(input, offset, rawData, 0, len)
                    val items = LogUtil.getStackTrace()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isDecrypt"),
                            Tip.getTip("rawData") + String(rawData),
                            Tip.getTip("decryptResult") + result
                        ) + items, HookHelper.hostPackageName
                    )
                    LogUtil.toLogMsg(Gson().toJson(logBean), logBean.type)
                }
            })
    }
}