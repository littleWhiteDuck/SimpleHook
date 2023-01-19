package me.simpleHook.hook

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import com.github.kyuubiran.ezxhelper.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.utils.LogUtil.noSuchMethod
import me.simpleHook.hook.utils.LogUtil.notFoundClass
import me.simpleHook.hook.utils.LogUtil.getStackTrace
import me.simpleHook.hook.utils.LogUtil.toLogMsg
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.utils.Type.getDataTypeValue
import me.simpleHook.hook.extension.*
import me.simpleHook.hook.utils.*
import me.simpleHook.util.*
import org.json.JSONObject


object MainHook {
    /* private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
     private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")*/
    /*    private val prefHookConfig by lazy { getHookConfigPref() }
        private val prefAssistConfig by lazy { getHookConfigPref("assistConfig") }*/

    fun startHook(packageName: String) {
        ConfigUtil.getConfigFromFile(packageName)?.let {
            "get custom config succeed".log(packageName)
            readHook(it, packageName)
        } ?: "get custom config failed".log(packageName)
        ConfigUtil.getConfigFromFile(packageName, Constant.EXTENSION_CONFIG_NAME)?.let {
            "get extension config succeed".log(packageName)
            readyExtensionHook(it, packageName)
        } ?: "get extension config failed".log(packageName)
    }

    /*  private fun xmlHook(
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

      }*/

    private fun readHook(strConfig: String, packageName: String) {
        if (strConfig.trim().isEmpty()) return
        try {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            if (!appConfig.enable) return
            val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
            val configs = Gson().fromJson<ArrayList<ConfigBean>>(appConfig.configs, listType)
            getTip("startCustomHook").log(packageName)
            configs.forEach {
                if (!it.enable) return@forEach
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD, Constant.HOOK_RECORD_STATIC_FIELD -> FieldHook.hookStaticField(
                            configBean = it, packageName
                        )
                        Constant.HOOK_FIELD, Constant.HOOK_RECORD_INSTANCE_FIELD -> FieldHook.hookInstanceField(
                            it, packageName = packageName
                        )
                        else -> specificHook(
                            className = className,
                            methodName = methodName,
                            values = resultValues,
                            params = params,
                            mode = mode,
                            packageName = packageName,
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
                ), packageName, "Error Unknown Error"
            )
            "config error".log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }
    }


    private fun specificHook(
        className: String,
        methodName: String,
        values: String,
        params: String,
        mode: Int,
        packageName: String,
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
                { hookParamsValue(it, values, className, methodName, params, packageName) }
            }
            Constant.HOOK_RECORD_PARAMS -> {
                { recordParamsValue(className, it, packageName) }
            }
            Constant.HOOK_RECORD_RETURN -> {
                { recordReturnValue(className, it, packageName) }
            }
            Constant.HOOK_RECORD_PARAMS_RETURN -> {
                { recordParamsAndReturn(className, it, packageName) }
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
                    hookAllConstructorAfter(className, hooker = hooker)
                } else {
                    findAllMethods(className) {
                        name == methodName
                    }.hook(mode, hooker)
                }
            } else {
                if (methodName == "<init>") {
                    findConstructor(className) {
                        isSearchConstructor(params)
                    }.hookAfter(hooker)
                } else {
                    findMethod(className) {
                        name == methodName && isSearchMethod(params)
                    }.hook(mode, hooker)
                }
            }
        } catch (e: NoSuchMethodError) {
            noSuchMethod(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: NoSuchMethodException) {
            noSuchMethod(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            notFoundClass(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            notFoundClass(
                packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }

    }

    private fun hookReturnValuePro(
        values: String, param: XC_MethodHook.MethodHookParam, returnClassName: String
    ) {
        val hookClass = XposedHelpers.findClass(returnClassName, ezXClassLoader)
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
        params: String,
        packageName: String
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
            LogUtil.toLog(list, packageName, "Error HookParamsError")
        }
    }

    private fun recordParamsValue(
        className: String, param: XC_MethodHook.MethodHookParam, packageName: String
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
        val logBean = LogBean(
            type, list + items, packageName
        )
        toLogMsg(Gson().toJson(logBean), packageName, type)
    }

    private fun recordReturnValue(
        className: String, param: XC_MethodHook.MethodHookParam, packageName: String
    ) {
        val list = mutableListOf<String>()
        val type = if (LanguageUtils.isNotChinese()) "Return value" else "返回值"
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + param.method.name)
        val result = getObjectString(param.result ?: "null")
        list.add(getTip("returnValue") + result)
        val items = getStackTrace()
        val logBean = LogBean(
            type, list + items, packageName
        )
        toLogMsg(Gson().toJson(logBean), packageName, type)
    }

    private fun recordParamsAndReturn(
        className: String, param: XC_MethodHook.MethodHookParam, packageName: String
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
        val logBean = LogBean(
            type, list + items, packageName
        )
        toLogMsg(Gson().toJson(logBean), packageName, type)
    }

    private fun getObjectString(value: Any): String {
        return if (value is String) value else try {
            Gson().toJson(value)
        } catch (e: java.lang.Exception) {
            value.javaClass.name
        }
    }

    private fun readyExtensionHook(
        strConfig: String, packageName: String
    ) {
        try {
            if (strConfig.trim().isEmpty()) return
            getTip("startExtensionHook").log(packageName)
            val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
            if (!configBean.all) return
            initExtensionHook(
                configBean,
                packageName,
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
                ApplicationHook
            )
        } catch (e: java.lang.Exception) {
            LogUtil.toLog(
                arrayListOf(
                    getTip("errorType") + getTip("unknownError"),
                    "config: ${JsonUtil.formatJson(strConfig)}",
                    getTip("detailReason") + e.stackTraceToString()
                ), packageName, "Error Unknown Error"
            )
        }
    }

    private fun initExtensionHook(
        configBean: ExtensionConfigBean, packageName: String, vararg hooks: BaseHook
    ) {
        hooks.forEach {
            it.startHook(configBean, packageName)
        }
    }
}

