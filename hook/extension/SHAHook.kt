package me.simpleHook.hook.extension

import android.content.Context
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.BaseHook
import me.simpleHook.hook.LogHook
import me.simpleHook.hook.Tip
import me.simpleHook.hook.byte2Sting
import java.security.MessageDigest

class SHAHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {
    override fun startHook(packageName: String, strConfig: String) {
        val hashMap = HashMap<String, String>()
        XposedBridge.hookAllMethods(MessageDigest::class.java, "update", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val paramLen = param.args.size
                if (paramLen == 1) {
                    when (val param0 = param.args[0]) {
                        is Byte -> {
                            val rawData = param0.toString()
                            hashMap["rawData"] = rawData
                        }
                        is ByteArray -> {
                            val rawData = String(param0)
                            hashMap["rawData"] = rawData
                        }
                    }
                } else if (paramLen == 3) {
                    val input = param.args[0] as ByteArray
                    val offset = param.args[1] as Int
                    val len = param.args[2] as Int
                    val rawData = ByteArray(len)
                    System.arraycopy(input, offset, rawData, 0, len)
                    hashMap["rawData"] = String(rawData)
                }
            }
        })

        XposedBridge.hookAllMethods(MessageDigest::class.java, "digest", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.args.size == 3) return
                if (param.args.size == 1) {
                    val data = param.args[0] as ByteArray
                    hashMap["rawData"] = String(data)
                }
                val md = param.thisObject as MessageDigest
                val type = md.algorithm ?: "unknown"
                val result = byte2Sting(param.result as ByteArray)
                val items = LogHook.getStackTrace().toList()
                val logBean = LogBean(
                    type, listOf(
                        Tip.getTip("isEncrypt"),
                        Tip.getTip("rawData") + hashMap["rawData"],
                        Tip.getTip("encryptResult") + result
                    ) + items, packageName
                )
                LogHook.toLogMsg(
                    mContext, Gson().toJson(logBean), packageName, logBean.type
                )
            }
        })
    }

}