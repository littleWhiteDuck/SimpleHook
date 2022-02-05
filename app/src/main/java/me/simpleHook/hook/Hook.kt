package me.simpleHook.hook

import android.annotation.SuppressLint
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.contentValuesOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.BuildConfig
import me.simpleHook.bean.*
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.Type.getDataTypeValue
import me.simpleHook.util.CipherUtils
import me.simpleHook.util.log
import me.simpleHook.util.tip
import java.io.File
import java.io.FileNotFoundException
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
private const val selfCheckConfig =
    "{\"appName\":\"\",\"configs\":\"[{\\\"className\\\":\\\"me.simpleHook.ui.activity.MainActivity\\\",\\\"fieldName\\\":\\\"\\\",\\\"fieldType\\\":\\\"\\\",\\\"methodName\\\":\\\"isModuleLive\\\",\\\"mode\\\":0,\\\"params\\\":\\\"\\\",\\\"resultValues\\\":\\\"true\\\"}]\",\"description\":\"\",\"enable\":true,\"id\":0,\"packageName\":\"me.simpleHook\",\"versionName\":\"\"}"

class Hook {
    private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
    private val printUri = Uri.parse("content://littleWhiteDuck/print_logs")
    private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")
    private val prefHookConfig by lazy { getHookConfigPref() }
    private val prefAssistConfig by lazy { getHookConfigPref("assistConfig") }
    private var mContext: Context? = null
    private lateinit var mClassLoader: ClassLoader

    fun initHook(loadPackageParam: XC_LoadPackage.LoadPackageParam?) {
        val packageName = loadPackageParam!!.packageName
        val classLoader = loadPackageParam.classLoader
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach", Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mContext = param.args[0] as Context
                    mClassLoader = mContext?.classLoader ?: classLoader
                    if (packageName == "me.simpleHook") {
                        startHook(selfCheckConfig, packageName)
                    } else {
                        //优先通过context辅助hook：dialog、toast等
                        contextAssistHook(packageName)
                        //优先读取文件配置准备hook
                        fileHook(packageName)
                    }
                }
            })
    }

    private fun fileHook(
        packageName: String
    ) {
        try {
            val strConfig =
                File("${Constant.CONFIG_DIRECTORY + packageName + "/config"}.json").reader()
                    .use { it.readText() }
            "从文件获取配置成功".tip()
            determineCan(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            "无运行中软件配置，或软件没有储存权限".tip()
            "准备使用xml获取配置".tip()
            xmlHook(packageName)
        }

    }

    private fun xmlHook(
        packageName: String
    ) {
        val error = "no have config or error"
        prefHookConfig?.let {
            val strConfig = it.getString(packageName, error)
            if (strConfig == null || strConfig == error) {
                error.log()
                "准备使用Context获取配置".log()
                contextHook(packageName)
            } else {
                // xml读取配置成功
                determineCan(strConfig, packageName)
            }
        } ?: contextHook(packageName)

    }

    private fun determineCan(strConfig: String, packageName: String) {
        val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
        if (appConfig.enable) {
            "开始自定义Hook".log()
            startHook(strConfig, packageName)
        }
    }

    @SuppressLint("Range")
    private fun contextHook(packageName: String) {
        mContext?.contentResolver?.query(uri, null, "packageName = ?", arrayOf(packageName), null)
            ?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("canUse")) == 1) {
                        val configString = getString(getColumnIndex("app_config"))
                        "配置获取成功(context)".log()
                        startHook(configString, packageName)
                    }
                }
                close()
            } ?: "cursor is null,获取配置失败".log()
    }

    private fun startHook(strConfig: String, packageName: String) {
        try {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
            if (appConfig.configs.startsWith("config://")) {
                appConfig.configs =
                    CipherUtils.decrypt(appConfig.configs.replace("config://", "")).toString()
            }
            val configs = Gson().fromJson<ArrayList<ConfigBean>>(appConfig.configs, listType)
            configs.forEach {
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD -> hookStaticField(
                            className,
                            mClassLoader,
                            fieldName,
                            resultValues,
                            fieldType
                        )
                        Constant.HOOK_FIELD -> hookField(
                            className,
                            mClassLoader,
                            fieldName,
                            resultValues,
                            fieldType
                        )
                        else -> specificHook(
                            className,
                            mClassLoader,
                            methodName,
                            resultValues,
                            params,
                            mode,
                            packageName
                        )
                    }
                }
            }
        } catch (e: Exception) {
            "resolver config error".log()
        }
    }

    private fun hookStaticField(
        className: String, classLoader: ClassLoader,
        fieldName: String, values: String, valueType: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
        when (val value = getDataTypeValue(values)) {
            is Byte -> XposedHelpers.setStaticByteField(clazz, fieldName, values.toByte())
            is Short -> XposedHelpers.setStaticShortField(
                clazz,
                fieldName,
                value
            )
            is Int -> XposedHelpers.setStaticIntField(clazz, fieldName, values.toInt())
            is Long -> XposedHelpers.setStaticLongField(clazz, fieldName, values.toLong())
            is Float -> XposedHelpers.setStaticFloatField(
                clazz,
                fieldName,
                value
            )
            is Double -> XposedHelpers.setStaticDoubleField(
                clazz,
                fieldName,
                value
            )
            is Boolean -> XposedHelpers.setStaticBooleanField(
                clazz,
                fieldName,
                value
            )
            is String -> XposedHelpers.setStaticObjectField(clazz, fieldName, value)
            else -> XposedHelpers.setStaticObjectField(clazz, fieldName, null)
        }

    }

    private fun hookField(
        className: String, classLoader: ClassLoader, fieldName: String, values: String,
        valueType: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
        XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val thisObj = param.thisObject
                when (val value = getDataTypeValue(values)) {
                    is Byte -> XposedHelpers.setByteField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Short -> XposedHelpers.setShortField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Int -> XposedHelpers.setIntField(thisObj, fieldName, value)
                    is Long -> XposedHelpers.setLongField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Float -> XposedHelpers.setFloatField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Double -> XposedHelpers.setDoubleField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is Boolean -> XposedHelpers.setBooleanField(
                        thisObj,
                        fieldName,
                        value
                    )
                    is String -> XposedHelpers.setObjectField(thisObj, fieldName, value)
                    else -> XposedHelpers.setObjectField(thisObj, fieldName, null)
                }
            }
        })
    }


    private fun specificHook(
        className: String, classLoader: ClassLoader, methodName: String, values: String,
        params: String, mode: Int, packageName: String
    ) {
        val methodParams = params.split(",")
        val realSize = if (params == "") 0 else methodParams.size
        val obj = arrayOfNulls<Any>(realSize + 1)
        for (i in methodParams.indices) {
            val classType = Type.getClassType(methodParams[i])
            if (classType == null) {
                obj[i] = methodParams[i]
            } else {
                obj[i] = classType
            }
        }
        when (mode) {
            Constant.HOOK_RETURN -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {

                        val originValue = param.result
                        val targetValue = getDataTypeValue(values)
                        param.result = targetValue
                        mContext?.let {
                            val logBean = LogBean(
                                "返回值",
                                listOf(
                                    "类名：${param.thisObject?.javaClass?.name ?: "未获取到"}",
                                    "方法名：${param.method?.name ?: "未获取到"}",
                                    "原返回值：${originValue}",
                                    "目标值：${targetValue}"
                                ), packageName
                            )
                            if (packageName != BuildConfig.APPLICATION_ID) toLogMsg(
                                Gson().toJson(
                                    logBean
                                ),
                                packageName, "返回值"
                            )
                        }
                    }
                }
            }
            Constant.HOOK_BREAK -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                        mContext?.let {
                            val logBean = LogBean(
                                "中断执行",
                                listOf(
                                    "类名：${param.thisObject.javaClass.name ?: "未获取到"}",
                                    "方法名：${param.method.name ?: "未获取到"}",
                                    "执行：此方法已拦截"
                                ), packageName
                            )
                            toLogMsg(Gson().toJson(logBean), packageName, "中断执行")
                        }
                    }
                }
            }
            Constant.HOOK_PARAM -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val arrayList = ArrayList<String>()
                        arrayList.add("类名：${param.thisObject.javaClass.name ?: "未获取到"}")
                        arrayList.add("方法名：${param.method.name ?: "未获取到"}")
                        for (i in methodParams.indices) {
                            if (values.split(",")[i] == "") {
                                arrayList.add("arg${i + 1}：${param.args[i]} -> 未修改")
                                continue
                            }
                            val targetValue = getDataTypeValue(values.split(",")[i])
                            param.args[i] = targetValue
                            arrayList.add("arg${i + 1}：${param.args[i]} -> $targetValue")
                        }
                        val logBean = LogBean("参数值", arrayList, packageName)
                        toLogMsg(Gson().toJson(logBean), packageName, "参数值")
                    }
                }
            }
        }
        XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
    }


    @SuppressLint("Range")
    private fun contextAssistHook(packageName: String) {
        var config = ""
        mContext?.contentResolver?.query(
            assistUri,
            null,
            "packageName = ?",
            arrayOf(packageName),
            null
        )?.apply {
            while (moveToNext()) {
                config = getString(getColumnIndex("config"))
            }
            close()
        } ?: fileAssistHook(packageName)
        if (config == "") return
        mContext?.also {
            readyAssistHook(config, packageName)
        }
    }

    private fun xmlAssistHook(packageName: String) {
        val error = "no have assistConfig or error"
        prefAssistConfig?.let {
            val strConfig = it.getString(packageName, error)
            if (strConfig == null || strConfig == error) {
                error.log()
            } else {
                // xml读取配置成功
                readyAssistHook(strConfig, packageName)
            }
        } ?: error.log()

    }

    private fun fileAssistHook(packageName: String) {
        try {
            val strConfig =
                File("${Constant.CONFIG_DIRECTORY + packageName}/assistConfig.json").reader()
                    .use { it.readText() }
            "获取辅助配置成功".log()
            readyAssistHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            "无运行中软件辅助配置，或软件没有储存权限".log()
            "准备从xml中获取辅助配置".log()
            xmlAssistHook(packageName)
        }

    }

    private fun readyAssistHook(
        strConfig: String,
        packageName: String
    ) {
        val configBean = Gson().fromJson(strConfig, AssistConfigBean::class.java)
        configBean.apply {

            if (!all) return
            hookDialog(dialog, diaCancel, packageName)
            if (toast) hookToast(packageName)
            hookPopupWindow(popup, popCancel, packageName)
            if (hotFix) hotFix(mContext!!, packageName)
            if (intent) hookIntent(packageName)
            if (click) hookOnClick(packageName)
//            if (xposed) hookXposedCheck()
            if (vpn) hookVpnCheck()
            if (base64) base64(packageName)
            if (digest) shaAndMD5(packageName)
            if (hmac) mac(packageName)
            if (crypt) aes(packageName)
        }
    }

/*    private fun hookXposedCheck() {

        XposedBridge.hookAllConstructors(File::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val pathName = param.args[0] as String
                if (pathName.contains("/maps")) {
                    param.result = null
                }
            }
        })
    }*/

    /*
        private fun hookWifi() {
            XposedHelpers.findAndHookMethod(System::class.java, "getProperty", String::class.java,
                object : XC_MethodHook(){
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        if (key == "http.proxyHost"){
                            param.result = ""
                        }else if (key == "http.proxyPort"){
                            param.result = "-1"
                        }
                        XposedBridge.log("system1, $key")
                    }
                })
            XposedHelpers.findAndHookMethod(Proxy::class.java, "getHost", Context::class.java,
                object : XC_MethodHook(){
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = ""
                        XposedBridge.log("system2, getHost")
                    }
                })
            XposedHelpers.findAndHookMethod(Proxy::class.java, "getPort", Context::class.java,
                object : XC_MethodHook(){
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = -1
                        XposedBridge.log("system, getPort")
                    }
                })
        }
    */
    private fun hookVpnCheck() {
        try {
            XposedHelpers.findAndHookMethod(
                "java.net.NetworkInterface",
                mContext!!.classLoader,
                "getName",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        super.beforeHookedMethod(param)
                        param.result = "are you ok"
                    }
                })
        } catch (e: Exception) {
            "hook vpnCheck error".tip()
        }
    }

    private fun hookToast(packageName: String) {
        try {
            XposedBridge.hookAllMethods(Toast::class.java, "show", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val type = "Toast"
                    val stackTrace = Throwable().stackTrace
                    val log = Gson().toJson(
                        LogBean(
                            type, toStackTrace(stackTrace), packageName
                        )
                    )
                    toLogMsg(log, packageName, type)
                }
            })
        } catch (e: Exception) {
            "hook toast error".tip()
        }

    }

    private fun hookPopupWindow(popupStack: Boolean, cancel: Boolean, packageName: String) {
        try {
            XposedBridge.hookAllMethods(
                PopupWindow::class.java,
                "showAtLocation",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        hookPopupWindowDetail(param, popupStack, cancel, packageName)
                    }
                })
            XposedBridge.hookAllMethods(
                PopupWindow::class.java,
                "showAsDropDown",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        hookPopupWindowDetail(param, popupStack, cancel, packageName)
                    }
                })
        } catch (e: Exception) {
            "hook popupWindow error".tip()
        }
    }

    private fun hookPopupWindowDetail(
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
            val type = "PopupWindow"
            val stackTrace = Throwable().stackTrace
            val log = Gson().toJson(
                LogBean(
                    type, toStackTrace(stackTrace), packageName
                )
            )
            toLogMsg(log, packageName, type)
        }
    }

    private fun hookDialog(isSwitch: Boolean, cancel: Boolean, packageName: String) {
        try {
            XposedBridge.hookAllMethods(Dialog::class.java, "show", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    if (cancel) {
                        val dialog = param?.thisObject as Dialog
                        dialog.setCancelable(true)
                    }
                    if (isSwitch) {
                        val type = "弹窗"
                        val stackTrace = Throwable().stackTrace
                        val log = Gson().toJson(
                            LogBean(
                                type, toStackTrace(stackTrace), packageName
                            )
                        )
                        toLogMsg(log, packageName, type)
                    }
                }
            })
        } catch (e: Exception) {
            "hook dialog error".tip()
        }
    }

    private fun hookOnClick(packageName: String) {
        try {
            XposedBridge.hookAllMethods(View::class.java, "performClick", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val type = "点击事件"
                    val stackTrace = Throwable().stackTrace
                    val log = Gson().toJson(
                        LogBean(
                            type, toStackTrace(stackTrace), packageName
                        )
                    )
                    toLogMsg(log, packageName, type)
                }
            })
        } catch (e: Exception) {
            "hook performClick error".tip()
        }
    }


    private fun toStackTrace(
        stackTrace: Array<StackTraceElement>
    ): ArrayList<String> {
        val items = ArrayList<String>()
        var notBug = 0
        for (element in stackTrace) {
            val className = element.className
            if (className.startsWith("me.simpleHook") || className.startsWith("littleWhiteDuck") || className.startsWith(
                    "de.robv.android.xposed"
                ) || className.contains("LspHooker") || className.contains("EdHooker") || className.startsWith(
                    "me.weishu"
                )
            ) continue
            if (notBug == 0) {
                items.add("调用堆栈：")
            }
            notBug++
            items.add("类：${element.className} -->方法：${element.methodName}(line：${element.lineNumber})")
        }
        return items
    }

    private fun base64(packageName: String) {
        XposedHelpers.findAndHookMethod(
            "java.util.Base64.Encoder",
            mClassLoader,
            "encode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mContext?.let {
                        val data = param.args[0] as ByteArray
                        val stackTrace = Throwable().stackTrace
                        val items = toStackTrace(stackTrace).toList()
                        val result = String(param.result as ByteArray)
                        val logBean = LogBean(
                            "base64",
                            listOf("类型：加密", "原始数据：${String(data)}", "加密结果：$result") + items,
                            packageName
                        )
                        toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "java.util.Base64.Decoder",
            mClassLoader,
            "decode",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mContext?.let {
                        val data = param.args[0] as ByteArray
                        val stackTrace = Throwable().stackTrace
                        val items = toStackTrace(stackTrace).toList()
                        val result = String(param.result as ByteArray)
                        val logBean = LogBean(
                            "base64",
                            listOf(
                                "加密/解密：解密",
                                "原始数据：${String(data)}",
                                "解密结果：$result"
                            ) + items,
                            packageName
                        )
                        toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                    }
                }
            })

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
                    val stackTrace = Throwable().stackTrace
                    val items = toStackTrace(stackTrace).toList()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64",
                        listOf(
                            "加密/解密：加密",
                            "原始数据：${String(rawData)}",
                            "加密结果：$result"
                        ) + items,
                        packageName
                    )
                    toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                }
            }
        )

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
                    val stackTrace = Throwable().stackTrace
                    val items = toStackTrace(stackTrace).toList()
                    val result = String(param.result as ByteArray, Charset.forName("US-ASCII"))
                    val logBean = LogBean(
                        "base64",
                        listOf(
                            "加密/解密：解密",
                            "原始数据：${String(rawData)}",
                            "解密结果：$result"
                        ) + items,
                        packageName
                    )
                    toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                }
            }
        )
    }

    private fun shaAndMD5(packageName: String) {
        val hashMap = HashMap<String, String>()
        XposedBridge.hookAllMethods(
            MessageDigest::class.java,
            "update",
            object : XC_MethodHook() {
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

        XposedBridge.hookAllMethods(MessageDigest::class.java,
            "digest",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mContext?.let {
                        if (param.args.size == 3) return
                        if (param.args.size == 1) {
                            val data = param.args[0] as ByteArray
                            hashMap["rawData"] = String(data)
                        }
                        val md = param.thisObject as MessageDigest
                        val type = md.algorithm ?: "未知类型"
                        val result = byte2Sting(param.result as ByteArray)
                        val stackTrace = Throwable().stackTrace
                        val items = toStackTrace(stackTrace).toList()
                        val logBean = LogBean(
                            type,
                            listOf(
                                "加密/解密：加密",
                                "原始数据：${hashMap.getValue("rawData")}",
                                "加密结果：$result"
                            ) + items,
                            packageName
                        )
                        toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                    }
                }
            })
    }

    private fun aes(packageName: String) {
        val map: HashMap<String, String> = HashMap()
        XposedBridge.hookAllConstructors(
            IvParameterSpec::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val ivParameterSpec = param.thisObject as IvParameterSpec
                    val iv = String(ivParameterSpec.iv)
                    map["iv"] = iv
                }
            }
        )
        XposedBridge.hookAllConstructors(
            SecretKeySpec::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val secretKeySpec = param.thisObject as SecretKeySpec
                    val keyAlgorithm = secretKeySpec.algorithm
                    val key = String(secretKeySpec.encoded)
                    map["keyAlgorithm"] = keyAlgorithm
                    map["key"] = key
                }
            }
        )
        XposedBridge.hookAllConstructors(
            EncodedKeySpec::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = String(param.args[0] as ByteArray)
                    map["key"] = key
                }
            }
        )

        XposedBridge.hookAllMethods(
            Cipher::class.java,
            "init",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val opmode = param.args[0] as Int
                    val cryptType = if (opmode == Cipher.ENCRYPT_MODE) "加密" else "解密"
                    map["cryptType"] = cryptType
                }
            })
        XposedBridge.hookAllMethods(
            Cipher::class.java,
            "update",
            object : XC_MethodHook() {
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
            }
        )

        XposedBridge.hookAllMethods(
            Cipher::class.java,
            "doFinal",
            object : XC_MethodHook() {
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
                                "加密/解密：${map.getValue("cryptType")}",
                                "密钥：${map.getValue("key")}",
                                "iv：${map["iv"] ?: "null"}",
                                "原始数据：${map["rawData"] ?: "null"}",
                                "${map.getValue("cryptType")}结果：${map.getValue("result")}"
                            )
                            val stackTrace = Throwable().stackTrace
                            val items = toStackTrace(stackTrace).toList()
                            val logBean = LogBean(map["algorithmType"]!!, list + items, packageName)
                            toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                            map.clear()
                        }
                    }
                }
            }
        )
    }

    private fun mac(packageName: String) {
        val hasMap = HashMap<String, String>()
        XposedBridge.hookAllMethods(
            Mac::class.java,
            "init",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val secretKeySpec = param.args[0] as SecretKeySpec
                    val key = String(secretKeySpec.encoded)
                    val keyAlgorithm = secretKeySpec.algorithm
                    hasMap["key"] = key
                    hasMap["keyAlgorithm"] = keyAlgorithm
                }
            }
        )
        XposedBridge.hookAllMethods(
            Mac::class.java,
            "update",
            object : XC_MethodHook() {
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
            }
        )
        XposedBridge.hookAllMethods(
            Mac::class.java,
            "doFinal",
            object : XC_MethodHook() {
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
                        "密钥：${hasMap.getValue("key")}",
                        "密钥算法：${hasMap.getValue("keyAlgorithm")}",
                        "原始数据：${hasMap["rawData"] ?: "null"}",
                        "加密结果：${hasMap.getValue("result")}"
                    )
                    val stackTrace = Throwable().stackTrace
                    val items = toStackTrace(stackTrace).toList()
                    val logBean = LogBean(hasMap["algorithmType"]!!, list + items, packageName)
                    toLogMsg(Gson().toJson(logBean), packageName, logBean.type)
                    hasMap.clear()
                }
            }
        )
    }


    /*
    常用启动activity时的intent信息
     */

    private fun hookIntent(packageName: String) {
        try {
            val classLoader = mContext!!.classLoader
            XposedHelpers.findAndHookMethod(
                ACTIVITY, classLoader,
                START_ACTIVITY, Intent::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent, packageName)
                    }
                })

            XposedHelpers.findAndHookMethod(
                CONTEXT_WRAPPER, classLoader,
                START_ACTIVITY, Intent::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent, packageName)
                    }
                })

            XposedHelpers.findAndHookMethod(
                CONTEXT_WRAPPER,
                classLoader, START_ACTIVITY,
                Intent::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent, packageName)
                    }
                })

            XposedHelpers.findAndHookMethod(
                ACTIVITY,
                classLoader, START_ACTIVITY_FOR_RESULT,
                Intent::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent, packageName)
                    }
                })
            XposedHelpers.findAndHookMethod(
                ACTIVITY,
                classLoader, START_ACTIVITY_FOR_RESULT,
                Intent::class.java,
                Int::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {

                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent, packageName)
                    }
                })
        } catch (e: Exception) {
            "hook intent error".tip()
        }
    }

    private fun saveLog(intent: Intent, packageName: String) {
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
        val logBean = LogBean("intent", listOf(configBean), packageName)
        toLogMsg(Gson().toJson(logBean), packageName, "intent")
    }

    private fun toLogMsg(log: String, packageName: String, type: String) {
        try {
            val contentValues =
                contentValuesOf(
                    "packageName" to packageName,
                    "log" to log,
                    "read" to 0,
                    "type" to type
                )
            mContext?.let {
                it.contentResolver?.insert(printUri, contentValues)
            }
        } catch (e: Exception) {
            "current error when save log".tip()
        }
    }

    private fun getHookConfigPref(path: String = "hookConfig"): SharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
        return if (pref.file.canRead()) pref else null
    }

    private fun hotFix(context: Context, packageName: String) {
        val dexFilePaths: MutableList<String> = mutableListOf()
        val fileTree: FileTreeWalk =
            File("/storage/emulated/0/Download/simpleHook/hotFix/$packageName/").walk()
        fileTree.maxDepth(1)//遍历目录层级为1，即无需检查子目录
            .filter { it.isFile } //只挑选出文件,不处理文件夹
            .filter { it.extension == "dex" } //选择扩展名为“png”的处理
            .forEach {//循环处理符合条件的文件
                dexFilePaths.add(it.absolutePath)
            }
        try {
            for (index in 0 until dexFilePaths.size) {
                val originalLoader = context.classLoader
                val classLoader = DexClassLoader(
                    dexFilePaths[index],
                    context.cacheDir.path,
                    null,
                    null
                )
                dexFilePaths[index].tip()
                val loaderClass: Class<*> = BaseDexClassLoader::class.java
                val pathListField = loaderClass.getDeclaredField("pathList")
                pathListField.isAccessible = true
                val pathListObject = pathListField[classLoader]
                val pathListClass: Class<*> = pathListObject.javaClass
                val dexElementsField = pathListClass.getDeclaredField("dexElements")
                dexElementsField.isAccessible = true
                val dexElementsObject = dexElementsField[pathListObject]
                val originalPathListObject = pathListField[originalLoader]
                val originalDexElementsObject = dexElementsField[originalPathListObject]

                //数组操作，把最新的补丁dex文件插入到最前面
                val oldLength = java.lang.reflect.Array.getLength(originalDexElementsObject)
                val newLength = java.lang.reflect.Array.getLength(dexElementsObject)
                val concatDexElementsObject = java.lang.reflect.Array.newInstance(
                    dexElementsObject.javaClass.componentType,
                    oldLength + newLength
                )
                for (i in 0 until newLength) {
                    java.lang.reflect.Array.set(
                        concatDexElementsObject,
                        i,
                        java.lang.reflect.Array.get(dexElementsObject, i)
                    )
                }
                for (i in 0 until oldLength) {
                    java.lang.reflect.Array.set(
                        concatDexElementsObject,
                        newLength + i,
                        java.lang.reflect.Array.get(originalDexElementsObject, i)
                    )
                }
                dexElementsField[originalPathListObject] = concatDexElementsObject
            }

            //全量替换伪代码 -> originalLoader.pathList.dexElements = classLoader.pathList.dexElement
            //增量替换伪代码 -> originalLoader.pathList.dexElements += classLoader.pathList.dexElement
        } catch (e: Exception) {
            "hot fix error".tip()
        }
    }

    private fun byte2Sting(bytes: ByteArray): String {
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