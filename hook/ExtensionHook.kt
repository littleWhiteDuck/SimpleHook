package me.simpleHook.hook

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtraBean
import me.simpleHook.bean.IntentBean
import me.simpleHook.bean.LogBean
import me.simpleHook.util.log
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.spec.EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val ACTIVITY = "android.app.Activity"
private const val CONTEXT_WRAPPER = "android.content.ContextWrapper"
private const val START_ACTIVITY = "startActivity"
private const val START_ACTIVITY_FOR_RESULT = "startActivityForResult"

object ExtensionHook {

    fun hookVpnCheck(context: Context) {
        XposedHelpers.findAndHookMethod(
            "java.net.NetworkInterface",
            context.classLoader,
            "getName",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    param.result = "are you ok"
                }
            })
    }

    fun hookToast(context: Context, packageName: String) {
        XposedBridge.hookAllMethods(Toast::class.java, "show", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val toast: Toast = param?.thisObject as Toast
                val list = mutableListOf<String>()
                // not test some cases
                try {
                    XposedHelpers.getObjectField(toast, "mText")?.also {
                        list.add("文本：$it")
                    }
                    val toastView = toast.view
                    if (toastView is ViewGroup) {
                        list += getAllTextView(toastView)
                    } else if (toastView is TextView) {
                        list.add("文本：" + toastView.text.toString())
                    }
                } catch (e: Exception) {
                    "$packageName: get toast info error".log()
                }
                val type = "Toast"
                val stackTrace = Throwable().stackTrace
                val log = Gson().toJson(
                    LogBean(
                        type, list + LogHook.toStackTrace(stackTrace), packageName
                    )
                )
                LogHook.toLogMsg(context, log, packageName, type)
            }
        })

    }

    fun hookPopupWindow(
        context: Context, popupStack: Boolean, cancel: Boolean, packageName: String
    ) {
        XposedBridge.hookAllMethods(
            PopupWindow::class.java,
            "showAtLocation",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    hookPopupWindowDetail(context, param, popupStack, cancel, packageName)
                }
            })
        XposedBridge.hookAllMethods(
            PopupWindow::class.java,
            "showAsDropDown",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    hookPopupWindowDetail(context, param, popupStack, cancel, packageName)
                }
            })
    }

    fun hookPopupWindowDetail(
        context: Context,
        param: XC_MethodHook.MethodHookParam?,
        popupStack: Boolean,
        cancel: Boolean,
        packageName: String
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        if (cancel) {
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
        }
        if (popupStack) {
            val list = mutableListOf<String>()
            val contentView = popupWindow.contentView
            if (contentView is ViewGroup) {
                list += getAllTextView(contentView)
            } else if (contentView is TextView) {
                list.add("文本：" + contentView.text.toString())
            }
            val type = "PopupWindow"
            val stackTrace = Throwable().stackTrace
            val log = Gson().toJson(
                LogBean(
                    type, list + LogHook.toStackTrace(stackTrace), packageName
                )
            )
            LogHook.toLogMsg(context, log, packageName, type)
        }
    }

    fun hookDialog(context: Context, isSwitch: Boolean, cancel: Boolean, packageName: String) {
        XposedBridge.hookAllMethods(Dialog::class.java, "show", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val dialog = param?.thisObject as Dialog
                if (cancel) {
                    dialog.setCancelable(true)
                }
                if (isSwitch) {
                    val list = mutableListOf<String>()
                    val type = "弹窗"
                    val dialogView: View? = dialog.window?.decorView
                    dialogView?.also {
                        if (it is ViewGroup) {
                            list += getAllTextView(it)
                        } else if (it is TextView) {
                            list.add("文本：" + it.text.toString())
                        }
                    }
                    val stackTrace = Throwable().stackTrace
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.toStackTrace(stackTrace), packageName
                        )
                    )
                    LogHook.toLogMsg(context, log, packageName, type)
                }
            }
        })
    }

    private fun getAllTextView(viewGroup: ViewGroup): List<String> {
        val list = mutableListOf<String>()
        viewGroup.children.forEach {
            when (it) {
                is Button -> {
                    if (it.text.toString().isNotEmpty()) {
                        list.add("按钮：" + it.text.toString())
                    }
                }
                is TextView -> {
                    if (it.text.toString().isNotEmpty()) {
                        list.add("文本：" + it.text.toString())
                    }
                }
                is ViewGroup -> {
                    list += getAllTextView(it)
                }
            }
        }
        return list
    }

    fun hookOnClick(context: Context, packageName: String) {
        XposedBridge.hookAllMethods(View::class.java, "performClick", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val list = mutableListOf<String>()
                    val type = "点击事件"
                    val view = param.thisObject as View
                    val viewType = view.javaClass.name ?: "未获取到"
                    val listenerInfoObject = XposedHelpers.getObjectField(view, "mListenerInfo")
                    val mOnClickListenerObject =
                        XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener")
                    val callbackType = mOnClickListenerObject.javaClass.name
                    val viewId =
                        if (view.id == View.NO_ID) "id：无ID" else "id： " + Integer.toHexString(view.id)
                    list.add("控件类型：$viewType")
                    list.add("回调类名：$callbackType")
                    list.add(viewId)
                    if (view is TextView) {
                        list.add("文本：" + view.text.toString())
                    } else if (view is ViewGroup) {
                        list += getAllTextView(view)
                    }
                    val stackTrace = Throwable().stackTrace
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.toStackTrace(stackTrace), packageName
                        )
                    )
                    LogHook.toLogMsg(context, log, packageName, type)
                } catch (e: Exception) {
                    "error: click".log()
                }
            }
        })
    }

    fun base64(context: Context, packageName: String) {
        XposedHelpers.findAndHookMethod(
            "java.util.Base64.Encoder",
            context.classLoader,
            "encode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(stackTrace)
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64",
                        listOf("类型：加密", "原始数据：${String(data)}", "加密结果：$result") + items,
                        packageName
                    )
                    LogHook.toLogMsg(
                        context, Gson().toJson(logBean), packageName, logBean.type
                    )
                }
            })

        XposedHelpers.findAndHookMethod(
            "java.util.Base64.Decoder",
            context.classLoader,
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
                            "加密/解密：解密", "原始数据：${String(data)}", "解密结果：$result"
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(
                        context, Gson().toJson(logBean), packageName, logBean.type
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
                            "加密/解密：加密", "原始数据：${String(rawData)}", "加密结果：$result"
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, logBean.type)
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
                            "加密/解密：解密", "原始数据：${String(rawData)}", "解密结果：$result"
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, logBean.type)
                }
            })
    }

    fun shaAndMD5(context: Context, packageName: String) {
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
                val type = md.algorithm ?: "未知类型"
                val result = byte2Sting(param.result as ByteArray)
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(stackTrace).toList()
                val logBean = LogBean(
                    type, listOf(
                        "加密/解密：加密", "原始数据：${hashMap["rawData"]}", "加密结果：$result"
                    ) + items, packageName
                )
                LogHook.toLogMsg(
                    context, Gson().toJson(logBean), packageName, logBean.type
                )
            }
        })
    }

    fun aes(context: Context, packageName: String) {
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
                val cryptType = if (opmode == Cipher.ENCRYPT_MODE) "加密" else "解密"
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
                        val result = String(it as ByteArray)
                        map["result"] = result
                        val list = listOf(
                            "加密/解密：${map["cryptType"]}",
                            "密钥：${map["key"]}",
                            "iv：${map["iv"]}",
                            "原始数据：${map["rawData"]}",
                            "${map["cryptType"] ?: "error"}结果：${map["result"]}"
                        )
                        val stackTrace = Throwable().stackTrace
                        val items = LogHook.toStackTrace(stackTrace).toList()
                        val logBean = LogBean(
                            map["algorithmType"]!!, list + items, packageName
                        )
                        LogHook.toLogMsg(
                            context, Gson().toJson(logBean), packageName, logBean.type
                        )
                        map.clear()
                    }
                }
            }
        })
    }

    fun mac(context: Context, packageName: String) {
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
                    "密钥：${hasMap["key"]}",
                    "密钥算法：${hasMap["keyAlgorithm"]}",
                    "原始数据：${hasMap["rawData"]}",
                    "加密结果：${hasMap["result"]}"
                )
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(stackTrace).toList()
                val logBean = LogBean(
                    hasMap["algorithmType"]!!, list + items, packageName
                )
                LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, logBean.type)
                hasMap.clear()
            }
        })
    }


    /*
    常用启动activity时的intent信息
     */

    fun hookIntent(context: Context, packageName: String) {
        val classLoader = context.classLoader
        XposedHelpers.findAndHookMethod(ACTIVITY,
            classLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(context, intent, packageName)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER,
            classLoader,
            START_ACTIVITY,
            Intent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(context, intent, packageName)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER,
            classLoader,
            START_ACTIVITY,
            Intent::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(context, intent, packageName)
                }
            })

        XposedHelpers.findAndHookMethod(ACTIVITY,
            classLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(context, intent, packageName)
                }
            })
        XposedHelpers.findAndHookMethod(ACTIVITY,
            classLoader,
            START_ACTIVITY_FOR_RESULT,
            Intent::class.java,
            Int::class.java,
            Bundle::class.java,
            object : XC_MethodHook() {

                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(context, intent, packageName)
                }
            })
    }

    fun saveLog(context: Context, intent: Intent, packName: String) {
        val className = intent.component?.className ?: ""
        val packageName = intent.component?.packageName ?: ""
        val action = intent.action ?: ""
        val data = intent.dataString ?: ""
        val extraList = ArrayList<ExtraBean>()
        val extras = intent.extras
        extras?.keySet()?.forEach {
            val type = when (extras.get(it)) {
                is Boolean -> "boolean"
                is String -> "string"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is Bundle -> "bundle"
                else -> "暂未统计" // maybe error
            }
            extraList.add(ExtraBean(type, it, extras.get(it).toString()))
        }
        val configBean = IntentBean(packageName, className, action, data, extraList)
        val logBean = LogBean(
            "intent", listOf(configBean), packName
        )
        LogHook.toLogMsg(context, Gson().toJson(logBean), packName, "intent")
    }

    fun byte2Sting(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            if (Integer.toHexString(0xFF and b.toInt()).length == 1) {
                sb.append("0")
            }
            sb.append(Integer.toHexString(0xFF and b.toInt()))
        }
        return sb.toString()
    }
}