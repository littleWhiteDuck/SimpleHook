package me.simpleHook.hook

import android.app.AndroidAppHelper
import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.*
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ExtensionConfig
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.extension.log
import me.simpleHook.extension.random
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.extension.*
import me.simpleHook.hook.util.*
import me.simpleHook.hook.util.HookHelper.appContext
import me.simpleHook.hook.util.HookHelper.hostPackageName
import me.simpleHook.hook.util.HookUtils.getObjectString
import me.simpleHook.hook.util.LogUtil.getStackTrace
import me.simpleHook.hook.util.LogUtil.outHookError
import me.simpleHook.hook.util.LogUtil.outLogMsg
import me.simpleHook.hook.util.Type.getDataTypeValue
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.LanguageUtils
import org.json.JSONObject


object MainHook {

    fun readyHook(strConfig: String) {
        if (strConfig.isBlank()) return
        try {
            val appConfig = Json.decodeFromString<AppConfig>(strConfig)
            if (!appConfig.enable) return
            val configs = Json.decodeFromString<List<ConfigBean>>(appConfig.configs)
            getTip("startCustomHook").log(hostPackageName)
            configs.forEach { configBean ->
                if (!configBean.enable) return@forEach
                configBean.apply {
                    when (configBean.mode) {
                        Constant.HOOK_STATIC_FIELD, Constant.HOOK_RECORD_STATIC_FIELD -> {
                            FieldHook.hookStaticField(configBean)
                        }
                        Constant.HOOK_FIELD, Constant.HOOK_RECORD_INSTANCE_FIELD -> {
                            FieldHook.hookInstanceField(configBean)
                        }
                        else -> specificHook(className = className,
                            methodName = methodName,
                            values = resultValues,
                            params = params,
                            mode = mode,
                            returnClassName = returnClassName)
                    }
                }
            }
        } catch (e: Throwable) {
            val configTemp = try {
                val appConfig = Json.decodeFromString<AppConfig>(strConfig)
                JsonUtil.formatJson(appConfig.configs)
            } catch (e: Throwable) {
                strConfig
            }
            LogUtil.outLog(arrayListOf(getTip("errorType") + getTip("unknownError"),
                "config: $configTemp",
                getTip("detailReason") + e.stackTraceToString()), "Error Unknown Error")
            "config error".log(hostPackageName)
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
                throw java.lang.IllegalStateException("读不懂配置")
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
        } catch (e: Throwable) {
            outHookError(className, "$methodName($params)", e)
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
                            .getSharedPreferences("me.simpleHook", Context.MODE_PRIVATE)
                        val oldTime = sp.getLong("time_$key", 0L)
                        val oldRandom = sp.getString("random_$key", defaultValue)
                        val currentTime = System.currentTimeMillis() / 1000
                        if (currentTime - updateTime >= oldTime) {
                            val result = randomSeed.random(len)
                            sp.edit().putString("random_$key", result).apply()
                            sp.edit().putLong("time_$key", currentTime).apply()
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
            val list = listOf(getTip("errorType") + "HookParamsError",
                getTip("solution") + getTip("paramsNotEqualValues"),
                getTip("filledClassName") + className,
                getTip("filledMethodParams") + "$methodName($params)",
                getTip("detailReason") + e.stackTraceToString())
            LogUtil.outLog(list, "Error HookParamsError")
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
        outLogMsg(logBean)
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
        outLogMsg(logBean)
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
        outLogMsg(logBean)
    }


    fun readyExtensionHook(
        strConfig: String
    ) {
        try {
            if (strConfig.trim().isEmpty()) return
            getTip("startExtensionHook").log(hostPackageName)
            val configBean = Json.decodeFromString<ExtensionConfig>(strConfig)
            if (!configBean.all) return
            if (configBean.tip) appContext.showToast(msg = "SimpleHook: StartHook")
            initExtensionHook(configBean,
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
                ClipboardHook,
                ApplicationHook,
                SignatureHook,
                ContactHook,
                SensorMangerHook,
                ADBHook,
                FileHook,
                ExitHook)
        } catch (e: Throwable) {
            LogUtil.outLog(arrayListOf(getTip("errorType") + getTip("unknownError"),
                "config: ${JsonUtil.formatJson(strConfig)}",
                getTip("detailReason") + e.stackTraceToString()), "Error Unknown Error")
        }
    }

    private fun initExtensionHook(
        configBean: ExtensionConfig, vararg hooks: BaseHook
    ) {
        hooks.forEach {
            if (it.isInit) return@forEach
            it.isInit
            it.startHook(configBean)
        }
    }

}

