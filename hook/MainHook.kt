package me.simpleHook.hook

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.ExtensionConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.ErrorTool.noSuchMethod
import me.simpleHook.hook.ErrorTool.notFoundClass
import me.simpleHook.hook.LogHook.toLogMsg
import me.simpleHook.hook.LogHook.getStackTrace
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.Type.getDataTypeValue
import me.simpleHook.hook.extension.*
import me.simpleHook.util.*
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

class MainHook(mClassLoader: ClassLoader, mContext: Context) : BaseHook(mClassLoader, mContext) {
    private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
    private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")
    /*    private val prefHookConfig by lazy { getHookConfigPref() }
        private val prefAssistConfig by lazy { getHookConfigPref("assistConfig") }*/

    override fun startHook(packageName: String, strConfig: String) {
        if (FlavorUtils.isNormal()) {
            // 普通版本
            fileHook2(packageName)
        } else {
            // root版本
            fileHook(packageName)
        }
        contextAssistHook(packageName)
    }

    private fun fileHook(
        packageName: String
    ) {
        try {
            val strConfig =
                File(Constant.CONFIG_MAIN_DIRECTORY + packageName + "/config/" + Constant.APP_CONFIG_NAME).reader()
                    .use { it.readText() }
            getTip("getConfigSuccessRoot").log(packageName)
            determineCan(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            getTip("failedGetConfigRoot").log(packageName)
            fileHook2(packageName)
            /*"准备使用xml获取配置".tip()
            xmlHook(packageName)*/
        }
    }

    private fun fileHook2(
        packageName: String
    ) {
        try {
            val strConfig =
                File(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.APP_CONFIG_NAME).reader()
                    .use { it.readText() }
            getTip("getConfigSuccessData").tip(packageName)
            determineCan(strConfig, packageName)
            if (!FlavorUtils.isNormal()) {
                getTip("useNormalVersion").tip(packageName)
            }
        } catch (e: FileNotFoundException) {
            getTip("failedGetConfigData").tip(packageName)
            contextHook(packageName)
            /*"准备使用xml获取配置".tip()
            xmlHook(packageName)*/
        }
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

    private fun determineCan(strConfig: String, packageName: String) {
        if (strConfig.trim().isNotEmpty()) {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            if (appConfig.enable) {
                getTip("startCustomHook").log(packageName)
                readHook(strConfig, packageName)
            }
        }
    }

    @SuppressLint("Range")
    private fun contextHook(packageName: String) {
        mContext.contentResolver?.query(uri, null, "packageName = ?", arrayOf(packageName), null)
            ?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("enable")) == 1) {
                        val configString = getString(getColumnIndex("config"))
                        val appConfig = AppConfig(
                            configs = configString,
                            packageName = packageName,
                            appName = "",
                            versionName = "",
                            description = ""
                        )
                        getTip("getConfigSuccessDB").log(packageName)
                        readHook(Gson().toJson(appConfig), packageName)
                        break
                    }
                }
                close()
            } ?: getTip("failedGetConfigDB").log(packageName)
    }

    private fun readHook(strConfig: String, packageName: String) {
        try {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
            val configs = Gson().fromJson<ArrayList<ConfigBean>>(appConfig.configs, listType)
            configs.forEach {
                if (!it.enable) return@forEach
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD, Constant.HOOK_RECORD_STATIC_FIELD -> FieldHook.hookStaticField(
                            className = className,
                            classLoader = mClassLoader,
                            methodName = methodName,
                            params = params,
                            fieldName = fieldName,
                            values = resultValues,
                            fieldClassName = fieldClassName,
                            context = mContext,
                            packageName = packageName,
                            hookPoint = hookPoint,
                            isRecord = it.mode == Constant.HOOK_RECORD_STATIC_FIELD
                        )
                        Constant.HOOK_FIELD, Constant.HOOK_RECORD_INSTANCE_FIELD -> FieldHook.hookField(
                            className = className,
                            classLoader = mClassLoader,
                            methodName = methodName,
                            params = params,
                            fieldName = fieldName,
                            values = resultValues,
                            context = mContext,
                            packageName = packageName,
                            hookPoint = hookPoint,
                            isRecord = it.mode == Constant.HOOK_RECORD_INSTANCE_FIELD
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
            ErrorTool.toLog(
                mContext, arrayListOf(
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
        val methodParams = params.split(",")
        val realSize = if (params == "" || params == "*") 0 else methodParams.size
        val obj = arrayOfNulls<Any>(realSize + 1)
        for (i in 0 until realSize) {
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
                        hookReturnValue(values, param)
                    }
                }
            }
            Constant.HOOK_RETURN2 -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        hookReturnValuePro(values, param, returnClassName)
                    }
                }
            }
            Constant.HOOK_BREAK -> {
                obj[realSize] = object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                        return null
                    }
                }
            }
            Constant.HOOK_PARAM -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        hookParamsValue(param, values, className, methodName, params, packageName)
                    }
                }
            }
            Constant.HOOK_RECORD_PARAMS -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        recordParamsValue(className, methodName, param, packageName)
                    }
                }
            }
            Constant.HOOK_RECORD_RETURN -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        recordReturnValue(className, methodName, param, packageName)
                    }
                }
            }
            Constant.HOOK_RECORD_PARAMS_RETURN -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        recordParamsAndReturn(className, methodName, param, packageName)
                    }
                }
            }
        }
        try {
            if (params == "*") {
                val hookClass = mClassLoader.loadClass(className)
                if (methodName == "<init>") {
                    XposedBridge.hookAllConstructors(hookClass, obj[realSize] as XC_MethodHook?)
                } else {
                    XposedBridge.hookAllMethods(
                        hookClass, methodName, obj[realSize] as XC_MethodHook?
                    )
                }
            } else {
                if (methodName == "<init>") {
                    XposedHelpers.findAndHookConstructor(className, mClassLoader, *obj)
                } else {
                    XposedHelpers.findAndHookMethod(className, mClassLoader, methodName, *obj)
                }
            }
        } catch (e: NoSuchMethodError) {
            noSuchMethod(
                mContext, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            notFoundClass(
                mContext, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            notFoundClass(
                mContext, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }

    }

    private fun hookReturnValuePro(
        values: String, param: XC_MethodHook.MethodHookParam, returnClassName: String
    ) {
        val hookClass = XposedHelpers.findClass(returnClassName, mClassLoader)
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
            ErrorTool.toLog(mContext, list, packageName, "Error HookParamsError")
        }
    }

    private fun recordParamsValue(
        className: String,
        methodName: String,
        param: XC_MethodHook.MethodHookParam,
        packageName: String
    ) {
        val type = if (isShowEnglish) "Param value" else "参数值"
        val list = mutableListOf<String>()
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + methodName)
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
        toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
    }

    private fun recordReturnValue(
        className: String,
        methodName: String,
        param: XC_MethodHook.MethodHookParam,
        packageName: String
    ) {
        val list = mutableListOf<String>()
        val type = if (isShowEnglish) "Return value" else "返回值"
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + methodName)
        val result = getObjectString(param.result ?: "null")
        list.add(getTip("returnValue") + result)
        val items = getStackTrace()
        val logBean = LogBean(
            type, list + items, packageName
        )
        toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
    }

    private fun recordParamsAndReturn(
        className: String,
        methodName: String,
        param: XC_MethodHook.MethodHookParam,
        packageName: String
    ) {
        val type = if (isShowEnglish) "Param&Return Value" else "参返"
        val list = mutableListOf<String>()
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + methodName)
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
        toLogMsg(mContext, Gson().toJson(logBean), packageName, type)
    }

    private fun getObjectString(value: Any): String {
        return if (value is String) value else try {
            Gson().toJson(value)
        } catch (e: java.lang.Exception) {
            value.javaClass.name
        }
    }

    @SuppressLint("Range")
    private fun contextAssistHook(packageName: String) {
        var config = ""
        mContext.contentResolver?.query(
            assistUri, null, "packageName = ?", arrayOf(packageName), null
        )?.apply {
            while (moveToNext()) {
                config = getString(getColumnIndex("config"))
            }
            close()
        } ?: run {
            getTip("failedGetExtensionConfigDB").log(packageName)
            if (FlavorUtils.isNormal()) fileExtensionHook2(packageName) else fileExtensionHook(
                packageName
            )
        }
        if (config == "") return
        getTip("getExtensionConfigSuccessDB").log(packageName)
        readyExtensionHook(config, packageName)
    }
/*
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

    }*/

    private fun fileExtensionHook(packageName: String) {
        try {
            val strConfig =
                File(Constant.CONFIG_MAIN_DIRECTORY + packageName + "/config/" + Constant.EXTENSION_CONFIG_NAME).reader()
                    .use { it.readText() }
            getTip("getExtensionConfigSuccessRoot").log(packageName)
            readyExtensionHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            getTip("failedGetExtensionConfigRoot").log(packageName)
            fileExtensionHook2(packageName)
            /* "准备从xml中获取扩展配置".log()
             xmlAssistHook(packageName)*/
        }

    }

    private fun fileExtensionHook2(packageName: String) {
        try {
            val strConfig =
                File(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.EXTENSION_CONFIG_NAME).reader()
                    .use { it.readText() }
            getTip("getExtensionConfigSuccessData").log(packageName)
            readyExtensionHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            getTip("getExtensionConfigSuccessData").log(packageName)
            /* "准备从xml中获取扩展配置".log()
             xmlAssistHook(packageName)*/
        }

    }

    private fun readyExtensionHook(
        strConfig: String, packageName: String
    ) {
        try {
            if (strConfig.trim().isEmpty()) return
            getTip("startExtensionHook").log(packageName)
            val configBean = Gson().fromJson(strConfig, ExtensionConfigBean::class.java)
            configBean.apply {
                if (!all) return
                if (dialog || diaCancel || stopDialog.enable) {
                    DialogHook(mClassLoader, mContext).startHook(packageName, strConfig)
                }
                if (popup || popCancel || stopDialog.enable) {
                    PopupWindowHook(mClassLoader, mContext).startHook(packageName, strConfig)
                }
                if (toast) ToastHook(mClassLoader, mContext).startHook(packageName, "")
                if (hotFix) HotFixHook(mClassLoader, mContext).startHook(packageName, "")
                if (intent) IntentHook(mClassLoader, mContext).startHook(packageName, "")
                if (click) ClickEventHook(mClassLoader, mContext).startHook(packageName, "")
                if (vpn) VpnCheckHook(mClassLoader, mContext).startHook(packageName, "")
                if (base64) Base64Hook(mClassLoader, mContext).startHook(packageName, "")
                if (digest) SHAHook(mClassLoader, mContext).startHook(packageName, "")
                if (hmac) HMACHook(mClassLoader, mContext).startHook(packageName, "")
                if (crypt) AESHook(mClassLoader, mContext).startHook(packageName, "")
                if (jsonObject || jsonArray) JSONHook(mClassLoader, mContext).startHook(
                    packageName, strConfig
                )
                if (webLoadUrl || webDebug) WebHook(mClassLoader, mContext).startHook(
                    packageName, strConfig
                )
                if (filterClipboard.enable) ClipboardFilterHook(mClassLoader, mContext).startHook(
                    packageName, strConfig
                )
                if (application) ApplicationHook(mClassLoader, mContext).startHook(packageName, "")
            }
        } catch (e: java.lang.Exception) {
            ErrorTool.toLog(
                mContext, arrayListOf(
                    getTip("errorType") + getTip("unknownError"),
                    "config: ${JsonUtil.formatJson(strConfig)}",
                    getTip("detailReason") + e.stackTraceToString()
                ), packageName, "Error Unknown Error"
            )
        }
    }

}