package me.simpleHook.hook.extension

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip
import java.security.spec.EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        val map: HashMap<String, String> = HashMap()
        XposedBridge.hookAllConstructors(IvParameterSpec::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val ivParameterSpec = param.thisObject as IvParameterSpec
                val iv = String(ivParameterSpec.iv)
                map["iv"] = iv
            }
        })
        XposedBridge.hookAllConstructors(SecretKeySpec::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val secretKeySpec = param.thisObject as SecretKeySpec
                val keyAlgorithm = secretKeySpec.algorithm
                val key = String(secretKeySpec.encoded)
                map["keyAlgorithm"] = keyAlgorithm
                map["key"] = key
            }
        })
        XposedBridge.hookAllConstructors(EncodedKeySpec::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val key = String(param.args[0] as ByteArray)
                map["key"] = key
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val opmode = param.args[0] as Int
                val cryptType =
                    if (opmode == Cipher.ENCRYPT_MODE) Tip.getTip("encrypt") else Tip.getTip("decrypt")
                map["cryptType"] = cryptType
            }
        })
        XposedBridge.hookAllMethods(Cipher::class.java, "update", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                /*
                byte[] update(byte[] input)
                byte[] update(byte[] input, int inputOffset, int inputLen)
                 */
                val paramLen = param.args.size
                if (paramLen == 1 || paramLen == 3) {
                    val input = param.args[0] as ByteArray
                    var inputOffset = 0
                    var inputLen = input.size
                    if (paramLen == 3) {
                        inputLen = param.args[1] as Int
                        inputOffset = param.args[2] as Int
                    }
                    val rawData = ByteArray(inputLen)
                    System.arraycopy(input, inputOffset, rawData, 0, inputLen)
                    map["rawData"] = String(rawData)
                }
            }
        })

        XposedBridge.hookAllMethods(Cipher::class.java, "doFinal", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                /*
                byte[] doFinal()
                byte[] doFinal(byte[] input)
                byte[] doFinal(byte[] input, int inputOffset, int inputLen)
                 */
                val paramLen = param.args.size
                if (paramLen == 0 || paramLen == 1 || paramLen == 3) {
                    val cipher = param.thisObject as Cipher
                    val algorithmType = cipher.algorithm
                    map["algorithmType"] = algorithmType
                    if (paramLen == 1) {
                        val rawData = String(param.args[0] as ByteArray)
                        map["rawData"] = rawData
                    } else if (paramLen == 3) {
                        val input = param.args[0] as ByteArray
                        val inputOffset = param.args[1] as Int
                        val inputLen = param.args[2] as Int
                        val rawData = ByteArray(inputLen)
                        System.arraycopy(input, inputOffset, rawData, 0, inputLen)
                        map["rawData"] = String(rawData)
                    }
                    param.result?.let {
                        if (map["key"] == null && map["cryptType"] == null) return
                        val result = String(it as ByteArray)
                        map["result"] = result
                        val list = listOf(
                            Tip.getTip("encryptOrDecrypt") + map["cryptType"],
                            Tip.getTip("key") + map["key"],
                            "iv：${map["iv"]}",
                            Tip.getTip("rawData") + map["rawData"],
                            Tip.getTip(map["cryptType"] ?: "error") + Tip.getTip("result") + map["result"]
                        )
                        val stackTrace = Throwable().stackTrace
                        val items = LogHook.toStackTrace(stackTrace).toList()
                        val logBean = LogBean(
                            map["algorithmType"] ?: "null", list + items, packageName
                        )
                        LogHook.toLogMsg(
                            mContext, Gson().toJson(logBean), packageName, logBean.type
                        )
                        map.clear()
                    }
                }
            }
        })
    }
}