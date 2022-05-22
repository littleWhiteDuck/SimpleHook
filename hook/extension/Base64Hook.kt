package me.simpleHook.hook.extension

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip
import java.nio.charset.Charset

class Base64Hook(classloader: ClassLoader, context: Context) : BaseHook(classloader, context) {

    override fun startHook(packageName: String, strConfig: String) {
        XposedHelpers.findAndHookMethod("java.util.Base64.Encoder",
            mClassLoader,
            "encode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(stackTrace)
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isEncrypt"),
                            Tip.getTip("rawData") + String(data),
                            Tip.getTip("encryptResult") + result
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(
                        mContext, Gson().toJson(logBean), packageName, logBean.type
                    )
                }
            })

        XposedHelpers.findAndHookMethod("java.util.Base64.Decoder",
            mClassLoader,
            "decode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(stackTrace).toList()
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isDecrypt"),
                            Tip.getTip("rawData") + String(data),
                            Tip.getTip("decryptResult") + result
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(
                        mContext, Gson().toJson(logBean), packageName, logBean.type
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
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(stackTrace).toList()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isEncrypt"),
                            Tip.getTip("rawData") + String(rawData),
                            Tip.getTip("encryptResult") + result
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, logBean.type)
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
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(stackTrace).toList()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64", listOf(
                            Tip.getTip("isDecrypt"),
                            Tip.getTip("rawData") + String(rawData),
                            Tip.getTip("decryptResult") + result
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(mContext, Gson().toJson(logBean), packageName, logBean.type)
                }
            })
    }
}