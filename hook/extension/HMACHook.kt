package me.simpleHook.hook.extension

import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.utils.LogUtil
import me.simpleHook.hook.Tip
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HMACHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean, packageName: String) {
        if (!configBean.hmac) return
        val hasMap = HashMap<String, String>()
        XposedBridge.hookAllMethods(Mac::class.java, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val secretKeySpec = param.args[0] as SecretKeySpec
                val key = String(secretKeySpec.encoded)
                val keyAlgorithm = secretKeySpec.algorithm
                hasMap["key"] = key
                hasMap["keyAlgorithm"] = keyAlgorithm
            }
        })
        XposedBridge.hookAllMethods(Mac::class.java, "update", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                /*
                void update(byte input)
                void update(byte[] input)
                void update(byte[] input, int offset, int len)
                 */
                val paramLen = param.args.size
                if (paramLen == 1) {
                    when (val param0 = param.args[0]) {
                        is Byte -> {
                            val rawData = param0.toString()
                            hasMap["rawData"] = rawData
                        }
                        is ByteArray -> {
                            val rawData = String(param0)
                            hasMap["rawData"] = rawData
                        }
                    }
                } else if (paramLen == 3) {
                    val input = param.args[0] as ByteArray
                    val offset = param.args[1] as Int
                    val len = param.args[2] as Int
                    val rawData = ByteArray(len)
                    System.arraycopy(input, offset, rawData, 0, len)
                    hasMap["rawData"] = String(rawData)
                }
            }
        })
        XposedBridge.hookAllMethods(Mac::class.java, "doFinal", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                /*
                byte[] doFinal()
                byte[] doFinal(byte[] input)
                 */
                val paramLen = param.args.size
                if (paramLen == 2) return
                if (paramLen == 1) {
                    val rawData = param.args[0] as ByteArray
                    hasMap["rawData"] = String(rawData)
                }
                val mac = param.thisObject as Mac
                val algorithmType = mac.algorithm
                hasMap["algorithmType"] = algorithmType
                val result = param.result as ByteArray
                hasMap["result"] = String(result)

                val list = listOf(
                    Tip.getTip("key") + hasMap["key"],
                    Tip.getTip("keyAlgorithm") + hasMap["keyAlgorithm"],
                    Tip.getTip("rawData") + hasMap["rawData"],
                    Tip.getTip("encryptResult") + hasMap["result"]
                )
                val items = LogUtil.getStackTrace().toList()
                val logBean = LogBean(
                    hasMap["algorithmType"] ?: "null", list + items, packageName
                )
                LogUtil.toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                hasMap.clear()
            }
        })
    }
}