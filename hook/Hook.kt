package me.simpleHook.hook

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.bean.AssistConfigBean
import me.simpleHook.bean.ConfigBean
import me.simpleHook.bean.LogBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.hook.ExtensionHook.aes
import me.simpleHook.hook.ExtensionHook.base64
import me.simpleHook.hook.ExtensionHook.hookDialog
import me.simpleHook.hook.ExtensionHook.hookIntent
import me.simpleHook.hook.ExtensionHook.hookOnClick
import me.simpleHook.hook.ExtensionHook.hookPopupWindow
import me.simpleHook.hook.ExtensionHook.hookToast
import me.simpleHook.hook.ExtensionHook.hookVpnCheck
import me.simpleHook.hook.ExtensionHook.mac
import me.simpleHook.hook.ExtensionHook.shaAndMD5
import me.simpleHook.hook.LogHook.toLogMsg
import me.simpleHook.hook.LogHook.toStackTrace
import me.simpleHook.hook.Type.getDataTypeValue
import me.simpleHook.util.FlavorUtils
import me.simpleHook.util.log
import me.simpleHook.util.tip
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

    fun initHook(loadPackageParam: XC_LoadPackage.LoadPackageParam?) {
        val packageName = loadPackageParam!!.packageName
        val classLoader = loadPackageParam.classLoader
        XposedHelpers.findAndHookMethod(
            Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mContext = param.args[0] as Context
                    mClassLoader = mContext?.classLoader ?: classLoader
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
            "$packageName: 从根目录文件获取自定义配置成功".tip()
            determineCan(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            "$packageName: 根目录储存文件无运行中软件自定义配置".tip()
            "$packageName: 准备从私有目录获取自定义配置".log()
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
            "$packageName: 从私有目录文件获取自定义配置成功".tip()
            determineCan(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            "$packageName: 私有目录储存文件无运行中软件自定义配置".tip()
            "$packageName: 准备使用Context获取自定义配置".log()
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
                "开始自定义Hook".log()
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
                        startHook(Gson().toJson(appConfig), packageName)
                        break
                    }
                }
                close()
            } ?: "cursor is null,获取自定义配置失败，请开启增加读取配置方式（Root写入配置）".log()
    }

    private fun startHook(strConfig: String, packageName: String) {
        try {
            val appConfig = Gson().fromJson(strConfig, AppConfig::class.java)
            val listType = object : TypeToken<ArrayList<ConfigBean>>() {}.type
            val configs = Gson().fromJson<ArrayList<ConfigBean>>(appConfig.configs, listType)
            configs.forEach {
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD -> FieldHook.hookStaticField(
                            className, mClassLoader, fieldName, resultValues, fieldType
                        )
                        Constant.HOOK_FIELD -> FieldHook.hookField(
                            className, mClassLoader, fieldName, resultValues, fieldType
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
            "config error".log()
            strConfig.log()
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
                        val targetValue = getDataTypeValue(values)
                        param.result = targetValue
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
                        for (i in methodParams.indices) {
                            if (values.split(",")[i] == "") continue
                            val targetValue = getDataTypeValue(values.split(",")[i])
                            param.args[i] = targetValue
                        }
                    }
                }
            }
            Constant.HOOK_RECORD_PARAMS -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.args.isEmpty()) return
                        val list = mutableListOf<String>()
                        list.add("类名：$className")
                        list.add("方法名：$methodName")
                        val paramLen = param.args.size
                        for (i in 0 until paramLen) {
                            list.add("参数${i + 1}：${getObjectString(param.args[i] ?: "null")}")
                        }
                        val items = toStackTrace(Throwable().stackTrace).toList()
                        val logBean = LogBean(
                            "参数值", list + items, packageName
                        )
                        toLogMsg(mContext, Gson().toJson(logBean), packageName, "参数值")
                    }
                }
            }
            Constant.HOOK_RECORD_RETURN -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val list = mutableListOf<String>()
                        list.add("类名：$className")
                        list.add("方法名：$methodName")
                        val result = getObjectString(param.result ?: "null")
                        list.add("返回值：$result")
                        val items = toStackTrace(Throwable().stackTrace).toList()
                        val logBean = LogBean(
                            "返回值", list + items, packageName
                        )
                        toLogMsg(mContext, Gson().toJson(logBean), packageName, "返回值")
                    }
                }
            }
            Constant.HOOK_RECORD_PARAMS_RETURN -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.args.isEmpty()) return
                        val list = mutableListOf<String>()
                        list.add("类名：$className")
                        list.add("方法名：$methodName")
                        val paramLen = param.args.size
                        for (i in 0 until paramLen) {
                            list.add("参数${i + 1}：${getObjectString(param.args[i] ?: "null")}")
                        }
                        val result = getObjectString(param.result ?: "null")
                        list.add("返回值：$result")
                        val items = toStackTrace(Throwable().stackTrace).toList()
                        val logBean = LogBean(
                            "参返", list + items, packageName
                        )
                        toLogMsg(mContext, Gson().toJson(logBean), packageName, "参返")
                    }
                }
            }
        }
        XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
    }

    private fun getObjectString(value: Any): String {
        return if (value is List<*> || value is Array<*>) {
            Gson().toJson(value)
        } else value.toString()
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
        } ?: if (FlavorUtils.isNormal()) fileAssistHook2(packageName) else fileAssistHook(
            packageName
        )
        if (config == "") return
        mContext?.also {
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
            "$packageName: 根目录获取扩展配置成功".log()
            readyAssistHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            "$packageName: 根目录储存文件无运行中软件扩展配置".log()
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
            "$packageName: 私有目录获取扩展配置成功".log()
            readyAssistHook(strConfig, packageName)
        } catch (e: FileNotFoundException) {
            "$packageName: 私有目录储存文件无运行中软件扩展配置".log()
            /* "准备从xml中获取扩展配置".log()
             xmlAssistHook(packageName)*/
        }

    }

    private fun readyAssistHook(
        strConfig: String, packageName: String
    ) {
        if (strConfig.trim().isEmpty()) return
        val configBean = Gson().fromJson(strConfig, AssistConfigBean::class.java)
        configBean.apply {
            if (!all) return
            val context: Context = mContext!!
            hookDialog(context, dialog, diaCancel, packageName)
            if (toast) hookToast(context, packageName)
            hookPopupWindow(context, popup, popCancel, packageName)
            if (hotFix) HotFix.startFix(context, packageName)
            if (intent) hookIntent(context, packageName)
            if (click) hookOnClick(context, packageName)
            if (vpn) hookVpnCheck(context)
            if (base64) base64(context, packageName)
            if (digest) shaAndMD5(context, packageName)
            if (hmac) mac(context, packageName)
            if (crypt) aes(context, packageName)
        }
    }
}