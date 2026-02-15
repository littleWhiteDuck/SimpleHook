package me.simpleHook.platform.hook.extension

import android.util.Base64
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.Base64Operation
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.RecordOutHelper

object Base64Hook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (!extensionConfig.algorithmConfig.base64) return
        runCatching {
            XposedHelpers.findAndHookMethod(
                "java.util.Base64.Encoder",
                HookHelper.appClassLoader,
                "encode",
                ByteArray::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val data = param.args[0] as ByteArray
                        val result = param.result as ByteArray
                        RecordOutHelper.outputBase64(
                            operation = Base64Operation.Encode,
                            rawData = data,
                            resultData = result
                        )
                    }
                })

            XposedHelpers.findAndHookMethod(
                "java.util.Base64.Decoder",
                HookHelper.appClassLoader,
                "decode",
                ByteArray::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val data = param.args[0] as ByteArray
                        val result = param.result as ByteArray
                        RecordOutHelper.outputBase64(
                            operation = Base64Operation.Decode,
                            rawData = data,
                            resultData = result
                        )
                    }
                })
        }
        XposedHelpers.findAndHookMethod(
            Base64::class.java,
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
                    val result = param.result as ByteArray
                    RecordOutHelper.outputBase64(
                        operation = Base64Operation.Encode,
                        rawData = rawData,
                        resultData = result
                    )
                }
            })

        XposedHelpers.findAndHookMethod(
            Base64::class.java,
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
                    val result = param.result as ByteArray
                    RecordOutHelper.outputBase64(
                        operation = Base64Operation.Decode,
                        rawData = rawData,
                        resultData = result
                    )
                }
            })
    }
}