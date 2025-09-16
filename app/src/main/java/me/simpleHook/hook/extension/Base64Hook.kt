package me.simpleHook.hook.extension

import android.util.Base64
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.LogBean
import me.simpleHook.hook.language.tip
import me.simpleHook.hook.util.HookHelper
import me.simpleHook.hook.util.LogUtil
import java.nio.charset.Charset

object Base64Hook : BaseHook() {

    override fun startHook(configBean: ExtensionConfig) {
        if (!configBean.base64) return
        runCatching {
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
                                tip.isEncrypt,
                                tip.rawData + String(data),
                                tip.encryptResult + result
                            ) + items, HookHelper.hostPackageName
                        )
                        LogUtil.outLogMsg(logBean)
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
                                tip.isDecrypt,
                                tip.rawData + String(data),
                                tip.decryptResult + result
                            ) + items, HookHelper.hostPackageName
                        )
                        LogUtil.outLogMsg(logBean)
                    }
                })
        }
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
                            tip.isEncrypt,
                            tip.rawData + String(rawData),
                            tip.encryptResult + result
                        ) + items, HookHelper.hostPackageName
                    )
                    LogUtil.outLogMsg(logBean)
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
                            tip.isDecrypt,
                            tip.rawData + String(rawData),
                            tip.decryptResult + result
                        ) + items, HookHelper.hostPackageName
                    )
                    LogUtil.outLogMsg(logBean)
                }
            })
    }
}