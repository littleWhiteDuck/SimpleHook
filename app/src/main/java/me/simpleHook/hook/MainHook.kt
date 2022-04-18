package me.simpleHook.hook

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.bean.AssistConfigBean
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.ErrorTool.noSuchMethod
import me.simpleHook.hook.ErrorTool.notFoundClass
import me.simpleHook.hook.ExtensionHook.aes
import me.simpleHook.hook.ExtensionHook.base64
import me.simpleHook.hook.ExtensionHook.hookDialog
import me.simpleHook.hook.ExtensionHook.hookIntent
import me.simpleHook.hook.ExtensionHook.hookJSONArray
import me.simpleHook.hook.ExtensionHook.hookJSONObject
import me.simpleHook.hook.ExtensionHook.hookOnClick
import me.simpleHook.hook.ExtensionHook.hookPopupWindow
import me.simpleHook.hook.ExtensionHook.hookToast
import me.simpleHook.hook.ExtensionHook.hookVpnCheck
import me.simpleHook.hook.ExtensionHook.hookWebDebug
import me.simpleHook.hook.ExtensionHook.hookWebLoadUrl
import me.simpleHook.hook.ExtensionHook.mac
import me.simpleHook.hook.ExtensionHook.shaAndMD5
import me.simpleHook.hook.LogHook.toLogMsg
import me.simpleHook.hook.LogHook.toStackTrace
import me.simpleHook.hook.Tip.getTip
import me.simpleHook.hook.Type.getDataTypeValue
import me.simpleHook.util.*
import java.io.File
import java.io.FileNotFoundException


private const val selfCheckConfig =
    "{\"appName\":\"\",\"configs\":\"[{\\\"className\\\":\\\"me.simpleHook.ui.activity.MainActivity\\\",\\\"fieldName\\\":\\\"\\\",\\\"fieldType\\\":\\\"\\\",\\\"methodName\\\":\\\"isModuleLive\\\",\\\"mode\\\":0,\\\"params\\\":\\\"\\\",\\\"resultValues\\\":\\\"true\\\"}]\",\"description\":\"\",\"enable\":true,\"id\":0,\"packageName\":\"me.simpleHook\",\"versionName\":\"\"}"

class Hook {
    private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
    private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")

    /*    private val prefHookConfig by lazy { getHookConfigPref() }
        private val prefAssistConfig by lazy { getHookConfigPref("assistConfig") }*/
    private var mContext: Context? = null
    private lateinit var mClassLoader: ClassLoader
    private var isNotChinese = false
    fun initHook(loadPackageParam: XC_LoadPackage.LoadPackageParam?) {
        val packageName = loadPackageParam!!.packageName
        val classLoader = loadPackageParam.classLoader
        XposedHelpers.findAndHookMethod(
            Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mContext = param.args[0] as Context
                    mClassLoader = mContext?.classLoader ?: classLoader
                    isNotChinese = LanguageUtils.isNotChinese()
                    if (packageName == "me.simpleHook") {
                        startHook(selfCheckConfig, packageName)
                    } else {
                        //优先通过context扩展hook：dialog、toast等
                        contextAssistHook(packageName)
                        //优先读取文件配置准备hook
                        if (FlavorUtils.isNormal()) {
                            fileHook2(packageName)
                        } else {
                            fileHook(packageName)
                        }

                    }


                }
            })
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
                startHook(strConfig, packageName)
            }
        }
    }

    @SuppressLint("Range")
    private fun contextHook(packageName: String) {
        mContext?.contentResolver?.query(uri, null, "packageName = ?", arrayOf(packageName), null)
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
                        startHook(Gson().toJson(appConfig), packageName)
                        break
                    }
                }
                close()
            } ?: getTip("failedGetConfigDB").log(packageName)
    }

    private fun startHook(strConfig: String, packageName: String) {
        try {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
            val configs = Gson().fromJson<ArrayList<ConfigBean>>(appConfig.configs, listType)
            configs.forEach {
                if (!it.enable) return@forEach
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD -> FieldHook.hookStaticField(
                            className,
                            mClassLoader,
                            methodName,
                            params,
                            fieldName,
                            resultValues,
                            fieldClassName,
                            mContext!!,
                            packageName
                        )
                        Constant.HOOK_FIELD -> FieldHook.hookField(
                            className,
                            mClassLoader,
                            methodName,
                            params,
                            fieldName,
                            resultValues,
                            mContext!!,
                            packageName
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
            val configTemp = try {
                val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
                JsonUtil.formatJson(appConfig.configs)
            } catch (e: java.lang.Exception) {
                strConfig
            }
            ErrorTool.toLog(
                mContext!!, arrayListOf(
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
        classLoader: ClassLoader,
        methodName: String,
        values: String,
        params: String,
        mode: Int,
        packageName: String
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
                val hookClass = classLoader.loadClass(className)
                if (methodName == "<init>") {
                    XposedBridge.hookAllConstructors(hookClass, obj[realSize] as XC_MethodHook?)
                } else {
                    XposedBridge.hookAllMethods(
                        hookClass, methodName, obj[realSize] as XC_MethodHook?
                    )
                }
            } else {
                if (methodName == "<init>") {
                    XposedHelpers.findAndHookConstructor(className, classLoader, *obj)
                } else {
                    XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
                }
            }
        } catch (e: NoSuchMethodError) {
            noSuchMethod(
                mContext!!, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("noSuchMethod").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: XposedHelpers.ClassNotFoundError) {
            notFoundClass(
                mContext!!, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        } catch (e: ClassNotFoundException) {
            notFoundClass(
                mContext!!, packageName, className, "$methodName($params)", e.stackTraceToString()
            )
            getTip("notFoundClass").log(packageName)
            XposedBridge.log(e.stackTraceToString())
        }

    }

    private fun hookReturnValue(
        values: String, param: XC_MethodHook.MethodHookParam
    ) {
        val targetValue = getDataTypeValue(values)
        param.result = targetValue
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
            ErrorTool.toLog(mContext!!, list, packageName, "Error HookParamsError")
        }
    }

    private fun recordParamsValue(
        className: String,
        methodName: String,
        param: XC_MethodHook.MethodHookParam,
        packageName: String
    ) {
        val type = if (isNotChinese) "Param value" else "参数值"
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
        val items = toStackTrace(mContext!!, Throwable().stackTrace).toList()
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
        val type = if (isNotChinese) "Return value" else "返回值"
        list.add(getTip("className") + className)
        list.add(getTip("methodName") + methodName)
        val result = getObjectString(param.result ?: "null")
        list.add(getTip("returnValue") + result)
        val items = toStackTrace(mContext!!, Throwable().stackTrace).toList()
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
        val type = if (isNotChinese) "Param&Return Value" else "参返"
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
        val items = toStackTrace(mContext!!, Throwable().stackTrace).toList()
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
        mContext?.contentResolver?.query(
            assistUri, null, "packageName = ?", arrayOf(packageName), null
        )?.apply {
            while (moveToNext()) {
                config = getString(getColumnIndex("config"))
            }
            close()
        } ?: run {
            getTip("failedGetExtensionConfigDB").log(packageName)
            if (FlavorUtils.isNormal()) fileAssistHook2(packageName) else fileAssistHook(
                packageName
            )
        }
        if (config == "") return
        mContext?.also {
            getTip("getExtensionConfigSuccessDB").log(packageName)
            readyAssistHook(config, packageName)
        }
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

    private fun fileAssistHook(packageName: String) {
        try {
            val strConfig =
                File(Constant.CONFIG_MAIN_DIRECTORY + packageName + "/config/" + Constant.EXTENSION_CONFIG_NAME).reader()
                    .use { it.readText() }
            getTip("getExtensionConfigSuccessRoot").log(packageName)
            readyAssistHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            getTip("failedGetExtensionConfigRoot").log(packageName)
            fileAssistHook2(packageName)
            /* "准备从xml中获取扩展配置".log()
             xmlAssistHook(packageName)*/
        }

    }

    private fun fileAssistHook2(packageName: String) {
        try {
            val strConfig =
                File(Constant.ANDROID_DATA_PATH + packageName + "/simpleHook/config/" + Constant.EXTENSION_CONFIG_NAME).reader()
                    .use { it.readText() }
            getTip("getExtensionConfigSuccessData").log(packageName)
            readyAssistHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            getTip("getExtensionConfigSuccessData").log(packageName)
            /* "准备从xml中获取扩展配置".log()
             xmlAssistHook(packageName)*/
        }

    }

    private fun readyAssistHook(
        strConfig: String, packageName: String
    ) {
        try {
            if (strConfig.trim().isEmpty()) return
            getTip("startExtensionHook").log(packageName)
            val configBean = Gson().fromJson(strConfig, AssistConfigBean::class.java)
            configBean.apply {
                if (!all) return
                val context: Context = mContext!!
                hookDialog(context, dialog, diaCancel, stopDialog, packageName)
                if (toast) hookToast(context, packageName)
                hookPopupWindow(context, popup, popCancel, stopDialog, packageName)
                if (hotFix) HotFix.startFix(context, packageName)
                if (intent) hookIntent(context, packageName)
                if (click) hookOnClick(context, packageName)
                if (vpn) hookVpnCheck(context)
                if (base64) base64(context, packageName)
                if (digest) shaAndMD5(context, packageName)
                if (hmac) mac(context, packageName)
                if (crypt) aes(context, packageName)
                if (jsonObject) hookJSONObject(context, packageName)
                if (jsonArray) hookJSONArray(context, packageName)
                if (webLoadUrl) hookWebLoadUrl(context, packageName)
                if (webDebug) hookWebDebug(context, packageName)
            }
        } catch (e: java.lang.Exception) {
            ErrorTool.toLog(
                mContext!!, arrayListOf(
                    getTip("errorType") + getTip("unknownError"),
                    "config: ${JsonUtil.formatJson(strConfig)}",
                    getTip("detailReason") + e.stackTraceToString()
                ), packageName, "Error Unknown Error"
            )
        }
    }
}