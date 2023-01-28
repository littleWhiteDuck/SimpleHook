package me.simpleHook.hook

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.BuildConfig
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.extension.*
import me.simpleHook.hook.utils.*
import me.simpleHook.hook.utils.HookHelper.hostPackageName
import me.simpleHook.hook.utils.LogUtil.getStackTrace
import me.simpleHook.hook.utils.LogUtil.noSuchMethod
import me.simpleHook.hook.utils.LogUtil.notFoundClass
import me.simpleHook.hook.utils.LogUtil.toLogMsg
import me.simpleHook.hook.utils.Type.getDataTypeValue
import me.simpleHook.util.*
import org.json.JSONArray
import org.json.JSONObject


object MainHook {

    private val prefHookConfig by lazy { getPref(Constant.CUSTOM_CONFIG_PREF) }
    private val prefExHookConfig by lazy { getPref(Constant.EXTENSION_CONFIG_PREF) }

    fun startHook(packageName: String) {
        if (BuildConfig.FLAVOR == "lite") {
            readyXmlHook()
        } else {
            var internalCount = 0
            ConfigUtil.getConfigFromFile()?.let {
                "get custom config succeed from file".log(packageName)
                readyHook(it)
            } ?: run {
                "get custom config failed from file".log(packageName)
                ConfigUtil.getCustomConfigFromDB()?.let {
                    "get custom config succeed from db".log(packageName)
                    readyHook(it)
                } ?: run {
                    "get custom config failed from db".log(packageName)
                    internalCount++
                }
            }
            ConfigUtil.getConfigFromFile(Constant.EXTENSION_CONFIG_NAME)?.let {
                "get extension config succeed from file".log(packageName)
                readyExtensionHook(it)
            } ?: run {
                "get extension config failed from file".log(packageName)
                ConfigUtil.getExConfigFromDB()?.let {
                    "get extension config succeed from db".log(packageName)
                    readyExtensionHook(it)
                } ?: run {
                    "get extension config failed from db".log(packageName)
                    internalCount++
                }
            }
            // 特殊情况, 仅支持自定义hook功能
            if (internalCount == 2) readyInternalConfigHook()
        }
    }

    private fun readyInternalConfigHook() {
        try {
            HookHelper.enableRecord = false
            val internalConfigs = AssetsUtil.getText(InitFields.moduleRes.assets.open("configs"))
                ?.replace(Regex("<---.*--->"), "")?.trim()
            if (internalConfigs?.isEmpty() == true) return
            val jsonArray = JSONArray(internalConfigs)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                if (jsonObject.optString("packageName") == hostPackageName) {
                    readyHook(jsonObject.toString())
                }
            }
        } catch (e: Throwable) {
            //e.stackTraceToString().log(hostPackageName)
        }

    }

    private fun readyXmlHook() {
        prefHookConfig?.let { sp ->
            sp.getString(hostPackageName, null)?.let {
                readyHook(it)
            } ?: "not have the custom config".log(hostPackageName)
        } ?: "null: XSharedPreferences".log(hostPackageName)
        prefExHookConfig?.let { sp ->
            sp.getString(hostPackageName, null)?.let {
                readyExtensionHook(it)
            } ?: "not have the extension config".log(hostPackageName)
        } ?: "null: XSharedPreferences".log(hostPackageName)
    }

    private fun readyHook(strConfig: String) {
        if (strConfig.trim().isEmpty()) return
        try {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            if (!appConfig.enable) return
            val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
            val configs = Gson().fromJson<ArrayList<ConfigBean>>(appConfig.configs, listType)
            getTip("startCustomHook").log(hostPackageName)
            configs.forEach {
                if (!it.enable) return@forEach
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD, Constant.HOOK_RECORD_STATIC_FIELD -> FieldHook.hookStaticField(
                            configBean = it
                        )
                        Constant.HOOK_FIELD, Constant.HOOK_RECORD_INSTANCE_FIELD -> FieldHook.hookInstanceField(
                            it
                        )
                        else -> specificHook(
                            className = className,
                            methodName = methodName,
                            values = resultValues,
                            params = params,
                            mode = mode,
                            returnClassName = returnClassName
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val configTemp = try {
                val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
                JsonUtil.formatJson(appConfig.configs)
            } catch (e: java.lang.Exception) {
                strConfig
            }
            LogUtil.toLog(
                arrayListOf(
                    getTip("errorType") + getTip("unknownError"),
                    "config: $configTemp",
                    getTip("detailReason") + e.stackTraceToString()
                ), "Error Unknown Error"
            )
            "config error".log(hostPackageName)
            XposedBridge.log(e.stackTraceToString())
        }
    }


    private fun specificHook(
        className: String,
        methodName: String,
        values: String,
        params: String,
        mode: Int,
        returnClassName: String
    ) {
        val hooker: Hooker = when (mode) {
            Constant.HOOK_RETURN -> {
                { hookReturnValue(values, it) }
            }
            Constant.HOOK_RETURN2 -> {
                { hookReturnValuePro(values, it, returnClassName) }
            }
            Constant.HOOK_BREAK -> {
                {}
            }
            Constant.HOOK_PARAM -> {
                { hookParamsValue(it, values, className, methodName, params) }
            }
            Constant.HOOK_RECORD_PARAMS -> {
                { recordParamsValue(className, it) }
            }
            Constant.HOOK_RECORD_RETURN -> {
                { recordReturnValue(className, it) }
            }
            Constant.HOOK_RECORD_PARAMS_RETURN -> {
                { recordParamsAndReturn(className, it) }
            }
            else -> {
                throw Exception("读不懂配置")
            }
        }
        try {
            if (methodName == "*") {
                findAllMethods(className) {
                    true
                }.hook(mode, hooker)
            } else if (params == "*") {
                if (methodName == "<init>") {
                    hookAllConstructorBefore(className, hooker = hooker)
                } else {
                    findAllMethods(className) {
                        name == methodName
                    }.hook(mode, hooker)
                }
            } else {
                if (methodName == "<init>") {
                    findConstructor(className) {
                        isSearchConstructor(params)
                    }.hookBefore(hooker)
                } else {
                    findMethod(className) {
                        name == methodName && isSearchMethod(params)
                    }.hook(mode, hooker)
                }
            }
        } catch (e: NoSuchMethodError) {
            noSuchMethod(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(hostPackageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: NoSuchMethodException) {
            noSuchMethod(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(hostPackageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            notFoundClass(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(hostPackageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            notFoundClass(
                className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(hostPackageName)
            XposedBridge.log(e.stackTraceToString())
        }

    }

    private fun hookReturnValuePro(
        values: String, param: XC_MethodHook.MethodHookParam, returnClassName: String
    ) {
        val hookClass = XposedHelpers.findClass(returnClassName, HookHelper.appClassLoader)
        try {
            val hookObject = Gson().fromJson(values, hookClass)
            param.result = hookObject
        } catch (e: Exception) {
            hookReturnValue(values, param)
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun hookReturnValue(
        values: String, param: XC_MethodHook.MethodHookParam
    ) {
        val targetValue = getDataTypeValue(values)
        if (targetValue is String) {
            try {
                val jsonObject = JSONObject(targetValue)
                if (jsonObject.has("random") && jsonObject.has("length") && jsonObject.has("key")) {
                    val randomSeed = jsonObject.optString("random", "a1b2c3d4e5f6g7h8i9k0l")
                    val len = jsonObject.optInt("length", 10)
                    val updateTime = jsonObject.optLong("updateTime", -1L)
                    val key = jsonObject.getString("key")
                    val defaultValue = jsonObject.optString("defaultValue")
                    if (updateTime == -1L) {
                        val result = randomSeed.random(len)
                        param.result = result
                    } else {
                        val sp = AndroidAppHelper.currentApplication()
                            .getSharedPreferences("me.simpleHook", Context.MODE_MULTI_PROCESS)
                        val oldTime = sp.getLong("time_$key", 0L)
                        val oldRandom = sp.getString("random_$key", defaultValue)
                        val currentTime = System.currentTimeMillis() / 1000
                        if (currentTime - updateTime >= oldTime) {
                            val result = randomSeed.random(len)
                            sp.edit().putString("random_$key", result).commit()
                            sp.edit().putLong("time_$key", currentTime).commit()
                            param.result = result
                        } else {
                            param.result = oldRandom
                        }
                    }
                }
            } catch (e: Exception) {
                param.result = targetValue
            }
        } else {
            param.result = targetValue
        }
    }

    private fun hookParamsValue(
        param: XC_MethodHook.MethodHookParam,
        values: String,
        className: String,
        methodName: String,
        params: String
    ) {
        try {
            for (i in param.args.indices) {
                if (values.split(",")[i] == "") continue
                val targetValue = getDataTypeValue(values.split(",")[i])
                param.args[i] = targetValue
            }
        } catch (e: java.lang.Exception) {
            val list = listOf(
                getTip("errorType") + "HookParamsError",
                getTip("solution") + getTip("paramsNotEqualValues"),
                getTip("filledClassName") + className,
                getTip("filledMethodParams") + "$methodName($params)",
                getTip("detailReason") + e.stackTraceToString()
            )
            LogUtil.toLog(list, "Error HookParamsError")
        }
    }

    private fun recordParamsValue(
        className: String, param: XC_MethodHook.MethodHookParam
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Param value" else "参数值"
        val list = mutableListOf<String>()
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + param.method.name)
        val paramLen = param.args.size
        if (paramLen == 0) {
            list.add(getTip("notHaveParams"))
        } else {
            for (i in 0 until paramLen) {
                list.add("${getTip("param")}${i + 1}: ${getObjectString(param.args[i] ?: "null")}")
            }
        }
        val items = getStackTrace()
        val logBean = LogBean(type, list + items, hostPackageName)
        toLogMsg(Gson().toJson(logBean), type)
    }

    private fun recordReturnValue(
        className: String, param: XC_MethodHook.MethodHookParam
    ) {
        val list = mutableListOf<String>()
        val type = if (LanguageUtils.isNotChinese()) "Return value" else "返回值"
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + param.method.name)
        val result = getObjectString(param.result ?: "null")
        list.add(getTip("returnValue") + result)
        val items = getStackTrace()
        val logBean = LogBean(type, list + items, hostPackageName)
        toLogMsg(Gson().toJson(logBean), type)
    }

    private fun recordParamsAndReturn(
        className: String, param: XC_MethodHook.MethodHookParam
    ) {
        val type = if (LanguageUtils.isNotChinese()) "Param&Return Value" else "参返"
        val list = mutableListOf<String>()
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + param.method.name)
        val paramLen = param.args.size
        if (paramLen == 0) {
            list.add(getTip("notHaveParams"))
        } else {
            for (i in 0 until paramLen) {
                list.add("${getTip("param")}${i + 1}: ${getObjectString(param.args[i] ?: "null")}")
            }
        }
        val result = getObjectString(param.result ?: "null")
        list.add(getTip("returnValue") + result)
        val items = getStackTrace()
        val logBean = LogBean(type, list + items, hostPackageName)
        toLogMsg(Gson().toJson(logBean), type)
    }

    private fun getObjectString(value: Any): String {
        return if (value is String) value else try {
            Gson().toJson(value)
        } catch (e: java.lang.Exception) {
            value.javaClass.name
        }
    }

    private fun readyExtensionHook(
        strConfig: String
    ) {
        try {
            if (strConfig.trim().isEmpty()) return
            getTip("startExtensionHook").log(hostPackageName)
            val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
            if (!configBean.all) return
            initExtensionHook(
                configBean,
                DialogHook,
                PopupWindowHook,
                ToastHook,
                HotFixHook,
                IntentHook,
                ClickEventHook,
                VpnCheckHook,
                Base64Hook,
                SHAHook,
                HMACHook,
                AESHook,
                JSONHook,
                WebHook,
                ClipboardFilterHook,
                ApplicationHook,
                SignatureHook,
                ContactHook
            )
        } catch (e: java.lang.Exception) {
            LogUtil.toLog(
                arrayListOf(
                    getTip("errorType") + getTip("unknownError"),
                    "config: ${JsonUtil.formatJson(strConfig)}",
                    getTip("detailReason") + e.stackTraceToString()
                ), "Error Unknown Error"
            )
        }
    }

    private fun initExtensionHook(
        configBean: ExtensionConfigBean, vararg hooks: BaseHook
    ) {
        hooks.forEach {
            if (it.isInit) return@forEach
            it.isInit
            it.startHook(configBean)
        }
    }


    private fun getPref(path: String): XSharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
        return if (pref.file.canRead()) pref else null
    }

}

