package me.simpleHook.hook

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ExtensionItemConfig
import me.simpleHook.bean.ExtraBean
import me.simpleHook.bean.IntentBean
import me.simpleHook.bean.LogBean
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.util.LanguageUtils
import me.simpleHook.util.log
import org.json.JSONArray
import org.json.JSONObject
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

    // 判断应用是否处在中文环境
    private val isShowEnglish = LanguageUtils.isNotChinese()

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
        XposedHelpers.findAndHookMethod(Toast::class.java, "show", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val list = mutableListOf<String>()
                // not test some cases
                val toast: Toast = param?.thisObject as Toast
                try {
                    XposedHelpers.getObjectField(toast, "mText")?.also {
                        list.add(getTip("text") + it)
                    }
                } catch (e: NoSuchFieldError) {
                    "toast error1".log(packageName)
                    try {
                        XposedHelpers.getObjectField(toast, "mNextView")?.also {
                            val toastView = it as View
                            if (toastView is ViewGroup) {
                                list += getAllTextView(toastView)
                            } else if (toastView is TextView) {
                                list.add(getTip("text") + toastView.text.toString())
                            }
                        }
                    } catch (e: NoSuchFieldError) {
                        "toast error2".log(packageName)
                    }
                }
                val type = "Toast"
                val stackTrace = Throwable().stackTrace
                val log = Gson().toJson(
                    LogBean(
                        type, list + LogHook.toStackTrace(context, stackTrace), packageName
                    )
                )
                LogHook.toLogMsg(context, log, packageName, type)
            }
        })

    }

    fun hookPopupWindow(
        context: Context,
        popupStack: Boolean,
        cancel: Boolean,
        stopDialog: ExtensionItemConfig,
        packageName: String
    ) {
        XposedBridge.hookAllMethods(PopupWindow::class.java,
            "showAtLocation",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    hookPopupWindowDetail(
                        context, param, popupStack, cancel, stopDialog, packageName
                    )
                }
            })
        XposedBridge.hookAllMethods(PopupWindow::class.java,
            "showAsDropDown",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    hookPopupWindowDetail(
                        context, param, popupStack, cancel, stopDialog, packageName
                    )
                }
            })
    }

    /**
     * @param stopDialog 数据类，enable记录是否开启，info获取相应信息
     */
    fun hookPopupWindowDetail(
        context: Context,
        param: XC_MethodHook.MethodHookParam?,
        popupStack: Boolean,
        cancel: Boolean,
        stopDialog: ExtensionItemConfig,
        packageName: String
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        if (cancel) {
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
        }
        val list = mutableListOf<String>()
        val contentView = popupWindow.contentView
        if (contentView is ViewGroup) {
            list += getAllTextView(contentView)
        } else if (contentView is TextView) {
            list.add(getTip("text") + contentView.text.toString())
        }

        val stackTrace = Throwable().stackTrace

        if (stopDialog.enable) {
            val showText = list.toString()
            val keyWords = stopDialog.info.split(",")
            keyWords.forEach {
                if (it.isNotEmpty() && showText.contains(it)) {
                    val type =
                        if (isShowEnglish) "PopupWindow(blocked display)" else "PopupWindow（已拦截）"
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.toStackTrace(context, stackTrace), packageName
                        )
                    )
                    LogHook.toLogMsg(context, log, packageName, type)
                    param.result = null
                    return
                }
            }
        } else if (popupStack) {
            val type = "PopupWindow"
            val log = Gson().toJson(
                LogBean(
                    type, list + LogHook.toStackTrace(context, stackTrace), packageName
                )
            )
            LogHook.toLogMsg(context, log, packageName, type)
        }
    }

    fun hookDialog(
        context: Context,
        stackSwitch: Boolean,
        cancel: Boolean,
        stopDialog: ExtensionItemConfig,
        packageName: String
    ) {
        XposedBridge.hookAllMethods(Dialog::class.java, "show", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val dialog = param?.thisObject as Dialog
                val list = mutableListOf<String>()
                val dialogView: View? = dialog.window?.decorView
                dialogView?.also {
                    if (it is ViewGroup) {
                        list += getAllTextView(it)
                    } else if (it is TextView) {
                        list.add(getTip("text") + it.text.toString())
                    }
                }
                if (cancel) {
                    dialog.setCancelable(true)
                }
                val stackTrace = Throwable().stackTrace
                if (stopDialog.enable) {
                    val showText = list.toString()
                    val keyWords = stopDialog.info.split(",")
                    keyWords.forEach {
                        if (it.isNotEmpty() && showText.contains(it)) {
                            dialog.dismiss()
                            val type = if (isShowEnglish) "Dialog(blocked display)" else "弹窗（已拦截）"
                            val log = Gson().toJson(
                                LogBean(
                                    type,
                                    list + LogHook.toStackTrace(context, stackTrace),
                                    packageName
                                )
                            )
                            LogHook.toLogMsg(context, log, packageName, type)
                            return
                        }
                    }
                } else if (stackSwitch) {
                    val type = if (isShowEnglish) "Dialog" else "弹窗"
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.toStackTrace(context, stackTrace), packageName
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
                        list.add(getTip("button") + it.text.toString())
                    }
                }
                is TextView -> {
                    if (it.text.toString().isNotEmpty()) {
                        list.add(getTip("text") + it.text.toString())
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
                    val type = if (isShowEnglish) "Click Event" else "点击事件"
                    val view = param.thisObject as View
                    val viewType = view.javaClass.name ?: "未获取到"
                    val listenerInfoObject = XposedHelpers.getObjectField(view, "mListenerInfo")
                    val mOnClickListenerObject =
                        XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener")
                    val callbackType = mOnClickListenerObject.javaClass.name
                    val viewId =
                        if (view.id == View.NO_ID) "id：NO ID" else "id： " + Integer.toHexString(view.id)
                    list.add(getTip("viewType") + viewType)
                    list.add(getTip("callbackType") + callbackType)
                    list.add(viewId)
                    if (view is TextView) {
                        list.add(getTip("text") + view.text.toString())
                    } else if (view is ViewGroup) {
                        list += getAllTextView(view)
                    }
                    val stackTrace = Throwable().stackTrace
                    val log = Gson().toJson(
                        LogBean(
                            type, list + LogHook.toStackTrace(context, stackTrace), packageName
                        )
                    )
                    LogHook.toLogMsg(context, log, packageName, type)
                } catch (e: Exception) {
                    "error: click".log(packageName)
                }
            }
        })
    }

    fun base64(context: Context, packageName: String) {
        XposedHelpers.findAndHookMethod("java.util.Base64.Encoder",
            context.classLoader,
            "encode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(context, stackTrace)
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64", listOf(
                            getTip("isEncrypt"),
                            getTip("rawData") + String(data),
                            getTip("encryptResult") + result
                        ) + items, packageName
                    )
                    LogHook.toLogMsg(
                        context, Gson().toJson(logBean), packageName, logBean.type
                    )
                }
            })

        XposedHelpers.findAndHookMethod("java.util.Base64.Decoder",
            context.classLoader,
            "decode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = param.args[0] as ByteArray
                    val stackTrace = Throwable().stackTrace
                    val items = LogHook.toStackTrace(context, stackTrace).toList()
                    val result = String(param.result as ByteArray)
                    val logBean = LogBean(
                        "base64", listOf(
                            getTip("isDecrypt"),
                            getTip("rawData") + String(data),
                            getTip("decryptResult") + result
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
                    val items = LogHook.toStackTrace(context, stackTrace).toList()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64", listOf(
                            getTip("isEncrypt"),
                            getTip("rawData") + String(rawData),
                            getTip("encryptResult") + result
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
                    val items = LogHook.toStackTrace(context, stackTrace).toList()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64", listOf(
                            getTip("isDecrypt"),
                            getTip("rawData") + String(rawData),
                            getTip("decryptResult") + result
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
                val type = md.algorithm ?: "unknown"
                val result = byte2Sting(param.result as ByteArray)
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(context, stackTrace).toList()
                val logBean = LogBean(
                    type, listOf(
                        getTip("isEncrypt"),
                        getTip("rawData") + hashMap["rawData"],
                        getTip("encryptResult") + result
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
                val cryptType =
                    if (opmode == Cipher.ENCRYPT_MODE) getTip("encrypt") else getTip("decrypt")
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
                            getTip("encryptOrDecrypt") + map["cryptType"],
                            getTip("key") + map["key"],
                            "iv：${map["iv"]}",
                            getTip("rawData") + map["rawData"],
                            getTip(map["cryptType"] ?: "error") + getTip("Result") + map["result"]
                        )
                        val stackTrace = Throwable().stackTrace
                        val items = LogHook.toStackTrace(context, stackTrace).toList()
                        val logBean = LogBean(
                            map["algorithmType"] ?: "null", list + items, packageName
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
                    getTip("key") + hasMap["key"],
                    getTip("keyAlgorithm") + hasMap["keyAlgorithm"],
                    getTip("rawData") + hasMap["rawData"],
                    getTip("encryptResult") + hasMap["result"]
                )
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(context, stackTrace).toList()
                val logBean = LogBean(
                    hasMap["algorithmType"] ?: "null", list + items, packageName
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
        XposedHelpers.findAndHookMethod(
            ACTIVITY,
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

    fun hookJSONObject(context: Context, packageName: String) {
        XposedBridge.hookAllMethods(JSONObject::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSON put" else "JSON 增加"
                val name = param.args[0] as String
                val value = getObjectString(param.args[1] ?: "null")
                val list = arrayListOf("Name: $name", "Value: $value")
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(context, stackTrace).toList()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
            }
        })

        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSON creation" else "JSON 创建"
                val jsonObject = param.thisObject
                val map: LinkedHashMap<String, Any> = XposedHelpers.getObjectField(
                    jsonObject, "nameValuePairs"
                ) as LinkedHashMap<String, Any>
                if (map.isEmpty()) return
                val value = Gson().toJson(map)
                val list = arrayListOf("Value: $value")
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(context, stackTrace).toList()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
            }
        })
    }

    fun hookJSONArray(context: Context, packageName: String) {

        XposedBridge.hookAllMethods(JSONArray::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSONArray put" else "JSONArray 增加"
                val name = param.args[0] as String
                val value = getObjectString(param.args[1] ?: "null")
                val list = arrayListOf("Name: $name", "Value: $value")
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(context, stackTrace).toList()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
            }
        })

        XposedBridge.hookAllConstructors(JSONArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = if (isShowEnglish) "JSONArray creation" else "JSONArray 创建"
                val jsonObject = param.thisObject
                val map: List<Any> = XposedHelpers.getObjectField(
                    jsonObject, "values"
                ) as List<Any>
                if (map.isEmpty()) return
                val value = Gson().toJson(map)
                val list = arrayListOf("Value: $value")
                val stackTrace = Throwable().stackTrace
                val items = LogHook.toStackTrace(context, stackTrace).toList()
                val logBean = LogBean(
                    type, list + items, packageName
                )
                LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
            }
        })
    }

    fun hookWebLoadUrl(context: Context, packageName: String) {
        XposedBridge.hookAllMethods(WebView::class.java, "loadUrl", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = "WEB"
                val url = param.args[0] as String
                val list = mutableListOf<String>()
                list.add("Url: $url")
                if (param.args.size == 2) {
                    val headers = Gson().toJson(param.args[1])
                    list.add("Header: $headers")
                }
                val logBean = LogBean(type, list, packageName)
                LogHook.toLogMsg(context, Gson().toJson(logBean), packageName, type)
            }
        })
    }

    fun hookWebDebug(context: Context, packageName: String) {
        val webClass = XposedHelpers.findClass("android.webkit.WebView", context.classLoader)
        XposedBridge.hookAllConstructors(webClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedHelpers.callStaticMethod(webClass, "setWebContentsDebuggingEnabled", true)
            }
        })
        XposedHelpers.findAndHookMethod(webClass,
            "setWebContentsDebuggingEnabled",
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = true
                }
            })
    }

    private fun getObjectString(value: Any): String {
        return if (value is String) value else try {
            Gson().toJson(value)
        } catch (e: java.lang.Exception) {
            value.javaClass.name
        }
    }
}