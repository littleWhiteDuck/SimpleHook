package littleWhiteDuck

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.contentValuesOf
import com.google.gson.Gson
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.simpleHook.BuildConfig
import me.simpleHook.bean.*
import me.simpleHook.constant.Constant
import me.simpleHook.hook.tinker.TinkerHook
import me.simpleHook.util.log
import java.io.File
import java.io.FileNotFoundException

object Hook {
    private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
    private val printUri = Uri.parse("content://littleWhiteDuck/print_logs")
    private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")
    private const val selfCheckConfig =
        "{\"appName\":\"simpleHook\",\"packageName\":\"me.simpleHook\",\"mode\":0,\"config\":[{\"className\":\"me.simpleHook.ui.activity.MainActivity\",\"methodName\":\"isModuleLive\",\"resultValues\":\"true\",\"mode\":0,\"params\":\"\",\"fieldName\":\"\",\"fieldType\":\"\"}]}"
    private val prefHookConfig by lazy { getHookConfigPref() }
    private val prefAssistConfig by lazy { getHookConfigPref("assistConfig") }

    fun readyHook(param: XC_LoadPackage.LoadPackageParam?) {
        val packageName = param!!.packageName
        val classLoader = param.classLoader
        if (packageName == "me.simpleHook") {
            startHook(selfCheckConfig, classLoader)
        } else {
            //优先通过context辅助hook：dialog、toast等
            contextAssistHook(packageName)
            //优先读取文件配置准备hook
            fileHook(packageName, classLoader)
        }
    }

    private fun fileHook(
        packageName: String,
        classLoader: ClassLoader
    ) {
        try {
            val strConfig =
                File("${Constant.CONFIG_DIRECTORY + packageName + "/config"}.json").reader()
                    .use { it.readText() }
            "获取配置成功".log()
            readyHook(strConfig, classLoader)
        } catch (e: FileNotFoundException) {
            "无运行中软件配置，或软件没有储存权限".log()
            "准备使用xml获取配置".log()
            xmlHook(packageName, classLoader)
        }

    }

    private fun xmlHook(
        currentPackageName: String,
        classLoader: ClassLoader,
    ) {
        val error = "no have config or error"
        prefHookConfig?.let {
            val strConfig = it.getString(currentPackageName, error)
            if (strConfig == null || strConfig == error) {
                error.log()
                "准备使用Context获取配置".log()
                toContextHook(currentPackageName)
            } else {
                // xml读取配置成功
                readyHook(strConfig, classLoader)
            }
        } ?: error.log()

    }

    private fun readyHook(strConfig: String, classLoader: ClassLoader) {
        val appConfigBean = Gson().fromJson(strConfig, AppConfigBean::class.java)
        if (appConfigBean.canUse) {
            if (appConfigBean.mode == Constant.HOOK_ORIGIN) {
                "开始hook（普通）".log()
                startHook(strConfig, classLoader)
            } else {
                "开始hook（加固）".log()
                specialHook(strConfig)
            }
        }
    }

    private fun toContextHook(currentPackageName: String) {
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val context: Context = param!!.args[0] as Context
                    context.classLoader.also {
                        toHook(currentPackageName, context, it)
                    }
                }
            }
        )
    }

    private fun toHook(packageName: String, context: Context, classLoader: ClassLoader) {
        context.contentResolver.query(uri, null, "packageName = ?", arrayOf(packageName), null)
            ?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("canUse")) == 1) {
                        val configString = getString(getColumnIndex("app_config"))
                        "配置获取成功(context)".log()
                        startHook(configString, classLoader)
                    }
                }
                close()
            } ?: "cursor is null,获取配置失败".log()
    }

    private fun specialHook(strConfig: String) {
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val context = param!!.args[0] as Context
                    context.classLoader.also {
                        startHook(strConfig, it)
                    }
                }
            }
        )
    }


    private fun startHook(strConfig: String, classLoader: ClassLoader) {
        val appConfigBean = Gson().fromJson(strConfig, AppConfigBean::class.java)
        try {
            appConfigBean.config.forEach {
                it.apply {
                    when (it.mode) {
                        Constant.HOOK_STATIC_FIELD -> hookStaticField(
                            className,
                            classLoader,
                            fieldName,
                            resultValues,
                            fieldType
                        )
                        Constant.HOOK_FIELD -> hookField(
                            className,
                            classLoader,
                            fieldName,
                            resultValues,
                            fieldType
                        )
                        else -> hook(className, classLoader, methodName, resultValues, params, mode)
                    }
                }
            }
        } catch (e: Exception) {
            "get config error".log()
        }
    }

    private fun hookStaticField(
        className: String, classLoader: ClassLoader,
        fieldName: String, values: String, valueType: String
    ) {
        val clazz: Class<*> = XposedHelpers.findClass(className, classLoader)
        when (valueType) {
            "byte", "b", "B" -> XposedHelpers.setStaticByteField(clazz, fieldName, values.toByte())
            "short", "s", "S" -> XposedHelpers.setStaticShortField(
                clazz,
                fieldName,
                values.toShort()
            )
            "int", "i", "I" -> XposedHelpers.setStaticIntField(clazz, fieldName, values.toInt())
            "long", "j", "J" -> XposedHelpers.setStaticLongField(clazz, fieldName, values.toLong())
            "float", "f", "F" -> XposedHelpers.setStaticFloatField(
                clazz,
                fieldName,
                values.toFloat()
            )
            "double", "d", "D" -> XposedHelpers.setStaticDoubleField(
                clazz,
                fieldName,
                values.toDouble()
            )
            "boolean", "z", "Z" -> XposedHelpers.setStaticBooleanField(
                clazz,
                fieldName,
                values.toBoolean()
            )
            "null" -> XposedHelpers.setStaticObjectField(clazz, fieldName, null)
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
                when (valueType) {
                    "byte", "b", "B" -> XposedHelpers.setByteField(
                        thisObj,
                        fieldName,
                        values.toByte()
                    )
                    "short", "s", "S" -> XposedHelpers.setShortField(
                        thisObj,
                        fieldName,
                        values.toShort()
                    )
                    "int", "i", "I" -> XposedHelpers.setIntField(thisObj, fieldName, values.toInt())
                    "long", "j", "J" -> XposedHelpers.setLongField(
                        thisObj,
                        fieldName,
                        values.toLong()
                    )
                    "float", "f", "F" -> XposedHelpers.setFloatField(
                        thisObj,
                        fieldName,
                        values.toFloat()
                    )
                    "double", "d", "D" -> XposedHelpers.setDoubleField(
                        thisObj,
                        fieldName,
                        values.toDouble()
                    )
                    "boolean", "z", "Z" -> XposedHelpers.setBooleanField(
                        thisObj,
                        fieldName,
                        values.toBoolean()
                    )
                    "null" -> XposedHelpers.setObjectField(thisObj, fieldName, null)
                }
            }
        })
    }


    private fun hook(
        className: String, classLoader: ClassLoader, methodName: String, values: String,
        params: String, mode: Int
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
                        param.result = Type.getDataTypeValue(values)
                    }
                }
            }
            Constant.HOOK_BREAK -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                    }
                }
            }
            Constant.HOOK_PARAM -> {
                obj[realSize] = object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        for (i in methodParams.indices) {
                            if (methodParams[i] == "") continue
                            param.args[i] = Type.getDataTypeValue(values.split(",")[i])
                        }
                    }
                }
            }
        }
        XposedHelpers.findAndHookMethod(className, classLoader, methodName, *obj)
    }


    private fun contextAssistHook(packageName: String) {
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val context: Context = param!!.args[0] as Context
                    var config = ""
                    context.contentResolver.query(
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
                    } ?: fileAssistHook(packageName, context.classLoader)
                    if (config == "") return
                    context.classLoader.also {
                        readyAssistHook(config, context, it, packageName)
                    }
                }
            }
        )
    }

    private fun xmlAssistHook(packageName: String, classLoader: ClassLoader) {
        val error = "no have assistConfig or error"
        prefAssistConfig?.let {
            val strConfig = it.getString(packageName, error)
            if (strConfig == null || strConfig == error) {
                error.log()
            } else {
                // xml读取配置成功
                readyAssistHook(strConfig, null, classLoader, packageName)
            }
        } ?: error.log()

    }

    private fun fileAssistHook(packageName: String, classLoader: ClassLoader) {
        try {
            val strConfig =
                File("${Constant.CONFIG_DIRECTORY + packageName}/assistConfig.json").reader()
                    .use { it.readText() }
            "获取辅助配置成功".log()
            readyAssistHook(strConfig, null, classLoader, packageName)
        } catch (e: FileNotFoundException) {
            "无运行中软件辅助配置，或软件没有储存权限".log()
            "准备从xml中获取辅助配置".log()
            xmlAssistHook(packageName, classLoader)
        }

    }

    private fun readyAssistHook(
        strConfig: String,
        context: Context?,
        classLoader: ClassLoader,
        packageName: String
    ) {
        val configBean = Gson().fromJson(strConfig, AssistConfigBean::class.java)
        configBean.apply {
            if (!all) return
            toHookDialog(context, dialog, diaCancel)
            if (toast) hookToast(context)
            if (popup) hookPopupWindow(context)
            if (tinker) TinkerHook.main(context, packageName)
            if (intent) hookIntent(classLoader, context)
            if (vpn) hookVpnCheck(classLoader)
            if (click) hookOnClick(context)
        }

    }

    private fun hookVpnCheck(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "java.net.NetworkInterface",
            classLoader,
            "getName",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    param.result = "are you ok"
                }
            })
    }

    private fun hookPopupWindow(context: Context?) {
        XposedBridge.hookAllMethods(
            PopupWindow::class.java,
            "showAtLocation",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val type = param?.thisObject?.javaClass?.name ?: "未知"
                    val stackTrace = Throwable().stackTrace
                    toStackTrace(type, stackTrace, context)
                }
            })
        XposedBridge.hookAllMethods(
            PopupWindow::class.java,
            "showAsDropDown",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val type = param?.thisObject?.javaClass?.name ?: "未知"
                    val stackTrace = Throwable().stackTrace
                    toStackTrace(type, stackTrace, context)
                }
            })
    }

    private fun hookToast(context: Context?) {
        XposedBridge.hookAllMethods(Toast::class.java, "show", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)
                val type = param?.thisObject?.javaClass?.name ?: "未知"
                val stackTrace = Throwable().stackTrace
                toStackTrace(type, stackTrace, context)
            }
        })
    }

    private fun toHookDialog(context: Context?, isSwitch: Boolean, cancel: Boolean) {
        XposedBridge.hookAllMethods(Dialog::class.java, "show", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)
                if (cancel) {
                    val dialog = param?.thisObject as Dialog
                    dialog.setCancelable(true)
                }
                if (isSwitch) {
                    val type = param?.thisObject?.javaClass?.name ?: "未知"
                    val stackTrace = Throwable().stackTrace
                    toStackTrace(type, stackTrace, context)
                }
            }
        })
    }

    private fun hookOnClick(context: Context?) {
        XposedBridge.hookAllMethods(View::class.java, "performClick", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)
                val type = param?.thisObject?.javaClass?.name ?: "未知"
                val stackTrace = Throwable().stackTrace
                toStackTrace(type, stackTrace, context)
            }
        })
    }


    private fun toStackTrace(
        type: String,
        stackTrace: Array<StackTraceElement>,
        context: Context?
    ) {
        val items = ArrayList<String>()
        for (i in stackTrace) {
            val className = i.className
            if (className.startsWith("me.simpleHook") || className.startsWith("littleWhiteDuck") || className.startsWith(
                    "de.robv.android.xposed"
                ) || className.contains("LspHooker") || className.contains("EdHooker") || className.startsWith(
                    "me.weishu"
                )
            ) continue
            items.add("类：${i.className} -->方法：${i.methodName}(line：${i.lineNumber})")
        }
        val log = Gson().toJson(LogBean(type, items))
        toLogMsg(context, log)
    }

    /*
    常用启动activity时的intent信息
     */

    private const val ACTIVITY = "android.app.Activity"
    private const val CONTEXT_WRAPPER = "android.content.ContextWrapper"
    private const val START_ACTIVITY = "startActivity"
    private const val START_ACTIVITY_FOR_RESULT = "startActivityForResult"
    private fun hookIntent(classLoader: ClassLoader, context: Context?) {
        XposedHelpers.findAndHookMethod(
            ACTIVITY, classLoader,
            START_ACTIVITY, Intent::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, context)
                }
            })

        XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER, classLoader,
            START_ACTIVITY, Intent::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    saveLog(intent, context)
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
                    saveLog(intent, context)
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
                    saveLog(intent, context)
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
                    saveLog(intent, context)
                }
            })
    }

    private fun saveLog(intent: Intent, context: Context?) {
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
        val logBean = LogBean("intent", arrayListOf(configBean))
        toLogMsg(context, Gson().toJson(logBean))
    }

    private fun toLogMsg(context: Context?, log: String) {
        try {
            val contentValues = contentValuesOf("packageName" to "unless", "log" to log)
            context?.let {
                it.contentResolver?.insert(printUri, contentValues)
            }
        } catch (e: Exception) {
            "current error when save log".log()
        }
    }

    private fun getHookConfigPref(path: String = "hookConfig"): SharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
        return if (pref.file.canRead()) pref else null
    }
}