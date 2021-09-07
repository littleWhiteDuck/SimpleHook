package littleWhiteDuck

import android.annotation.SuppressLint
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Proxy
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.View
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.contentValuesOf
import com.google.gson.Gson
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
import me.simpleHook.util.log
import me.simpleHook.util.tip
import java.io.File
import java.io.FileNotFoundException

private const val ACTIVITY = "android.app.Activity"
private const val CONTEXT_WRAPPER = "android.content.ContextWrapper"
private const val START_ACTIVITY = "startActivity"
private const val START_ACTIVITY_FOR_RESULT = "startActivityForResult"
private const val selfCheckConfig =
    "{\"appName\":\"simpleHook\",\"packageName\":\"me.simpleHook\",\"mode\":0,\"config\":[{\"className\":\"me.simpleHook.ui.activity.MainActivity\",\"methodName\":\"isModuleLive\",\"resultValues\":\"true\",\"mode\":0,\"params\":\"\",\"fieldName\":\"\",\"fieldType\":\"\"}]}"

class Hook {
    private val uri = Uri.parse("content://littleWhiteDuck/app_configs")
    private val printUri = Uri.parse("content://littleWhiteDuck/print_logs")
    private val assistUri = Uri.parse("content://littleWhiteDuck/assist_configs")
    private val prefHookConfig by lazy { getHookConfigPref() }
    private val prefAssistConfig by lazy { getHookConfigPref("assistConfig") }
    private var mContext: Context? = null
    private lateinit var mClassLoader: ClassLoader

    fun toHook(loadPackageParam: XC_LoadPackage.LoadPackageParam?) {
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
                        startHook(selfCheckConfig)
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
            "获取配置成功".log()
            toHook(strConfig)
        } catch (e: FileNotFoundException) {
            "无运行中软件配置，或软件没有储存权限".log()
            "准备使用xml获取配置".log()
            xmlHook(packageName)
        }

    }

    private fun xmlHook(
        currentPackageName: String
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
                toHook(strConfig)
            }
        } ?: error.log()

    }

    private fun toHook(strConfig: String) {
        val appConfigBean = Gson().fromJson(strConfig, AppConfigBean::class.java)
        if (appConfigBean.canUse) {
            "开始自定义Hook".log()
            startHook(strConfig)
        }
    }

    private fun toContextHook(currentPackageName: String) {
        toHook(currentPackageName, mContext)
    }

    @SuppressLint("Range")
    private fun toHook(packageName: String, context: Context?) {
        context?.contentResolver?.query(uri, null, "packageName = ?", arrayOf(packageName), null)
            ?.apply {
                while (moveToNext()) {
                    if (getInt(getColumnIndex("canUse")) == 1) {
                        val configString = getString(getColumnIndex("app_config"))
                        "配置获取成功(context)".log()
                        startHook(configString)
                    }
                }
                close()
            } ?: "cursor is null,获取配置失败".log()
    }


    private fun startHook(strConfig: String) {
        val appConfigBean = Gson().fromJson(strConfig, AppConfigBean::class.java)
        try {
            appConfigBean.config.forEach {
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
                        else -> hook(
                            className,
                            mClassLoader,
                            methodName,
                            resultValues,
                            params,
                            mode
                        )
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
            hookDialog(dialog, diaCancel)
            if (toast) hookToast()
            hookPopupWindow(popup, popCancel)
            if (tinker) hotFix(mContext!!, packageName)
            if (intent) hookIntent()
            if (vpn) hookVpnCheck()
            if (click) hookOnClick()
        }
        hookXposedCheck()
    }

    private fun hookXposedCheck() {
        XposedBridge.hookAllConstructors(File::class.java, object :XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                val pathName = param.args[0] as String
                val pid = Process.myPid()
                XposedBridge.log("================-pid:$pid; $pathName;========")
                if (pathName.contains("maps")){
                    XposedBridge.log("====================执行========")
                    param.args[0] = if (pid > 0){
                        "/proc/$pid/anti_maps"
                    }else{
                        "/proc/self/anti_maps"
                    }
                    XposedBridge.log("====================-${param.args[0]}========")
                }
            }
        })
    }

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

    private fun hookToast() {
        try {
            XposedBridge.hookAllMethods(Toast::class.java, "show", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val type = param?.thisObject?.javaClass?.name ?: "未知"
                    val stackTrace = Throwable().stackTrace
                    toStackTrace(type, stackTrace)
                }
            })
        } catch (e: Exception) {
            "hook toast error".tip()
        }

    }

    private fun hookPopupWindow(popupStack: Boolean, cancel: Boolean) {
        try {
            XposedBridge.hookAllMethods(
                PopupWindow::class.java,
                "showAtLocation",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        hookPopupWindowDetail(param, popupStack, cancel)
                    }
                })
            XposedBridge.hookAllMethods(
                PopupWindow::class.java,
                "showAsDropDown",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        hookPopupWindowDetail(param, popupStack, cancel)
                    }
                })
        } catch (e: Exception) {
            "hook popupWindow error".tip()
        }
    }

    private fun hookPopupWindowDetail(
        param: XC_MethodHook.MethodHookParam?,
        popupStack: Boolean,
        cancel: Boolean
    ) {
        val popupWindow = param?.thisObject as PopupWindow
        if (cancel) {
            popupWindow.isFocusable = true
            popupWindow.isOutsideTouchable = true
        }
        if (popupStack) {
            val type = popupWindow.javaClass.name ?: "未知"
            val stackTrace = Throwable().stackTrace
            toStackTrace(type, stackTrace)
        }
    }

    private fun hookDialog(isSwitch: Boolean, cancel: Boolean) {
        try {
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
                        toStackTrace(type, stackTrace)
                    }
                }
            })
        } catch (e: Exception) {
            "hook dialog error".tip()
        }
    }

    private fun hookOnClick() {
        try {
            XposedBridge.hookAllMethods(View::class.java, "performClick", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val type = param?.thisObject?.javaClass?.name ?: "未知"
                    val stackTrace = Throwable().stackTrace
                    toStackTrace(type, stackTrace)
                }
            })
        } catch (e: Exception) {
            "hook performClick error".tip()
        }
    }


    private fun toStackTrace(
        type: String,
        stackTrace: Array<StackTraceElement>
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
        toLogMsg(log)
    }

    /*
    常用启动activity时的intent信息
     */

    private fun hookIntent() {
        try {
            val classLoader = mContext!!.classLoader
            XposedHelpers.findAndHookMethod(
                ACTIVITY, classLoader,
                START_ACTIVITY, Intent::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent)
                    }
                })

            XposedHelpers.findAndHookMethod(CONTEXT_WRAPPER, classLoader,
                START_ACTIVITY, Intent::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as Intent
                        saveLog(intent)
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
                        saveLog(intent)
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
                        saveLog(intent)
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
                        saveLog(intent)
                    }
                })
        } catch (e: Exception) {
            "hook intent error".tip()
        }
    }

    private fun saveLog(intent: Intent) {
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
        toLogMsg(Gson().toJson(logBean))
    }

    private fun toLogMsg(log: String) {
        try {
            val contentValues = contentValuesOf("packageName" to "unless", "log" to log)
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
        try {
            val originalLoader = context.classLoader
            val classLoader = DexClassLoader(
                "/storage/emulated/0/Download/simpleHook/hotFix/$packageName/hotfix.dex",
                context.cacheDir.path,
                null,
                null
            )
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

            //全量替换伪代码 -> originalLoader.pathList.dexElements = classLoader.pathList.dexElement
            //增量替换伪代码 -> originalLoader.pathList.dexElements += classLoader.pathList.dexElement
        } catch (e: Exception) {
            "hot fix error".tip()
        }
    }
}