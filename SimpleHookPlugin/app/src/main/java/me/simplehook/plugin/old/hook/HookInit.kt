package me.simplehook.plugin.old.hook

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.findAllConstructors
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorBefore
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.google.gson.Gson
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.serialization.json.Json
import me.simplehook.plugin.old.data.AppConfig
import me.simplehook.plugin.old.data.HookConfig
import me.simplehook.plugin.old.hook.Type.getDataTypeValue
import me.simplehook.plugin.util.AssetsUtil
import org.json.JSONArray
import org.json.JSONObject

class HookInit : IXposedHookLoadPackage, IXposedHookZygoteInit {
    private var config: String? = null
    private val gson by lazy(LazyThreadSafetyMode.NONE) { Gson() }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (InitFields.isAppContextInited) return
        EzXHelperInit.initHandleLoadPackage(lpparam)
        readConfig()
        findMethod(Application::class.java) {
            name == "attach"
        }.hookAfter {
            val context = it.args[0] as Context
            EzXHelperInit.initAppContext(context)
            EzXHelperInit.setEzClassLoader(context.classLoader)
            startHook(config)
        }
    }

    private fun readConfig() {
        runCatching {
            val internalConfigs = AssetsUtil.getText(InitFields.moduleRes.assets.open("configs.xml"))
                ?.replace(Regex("<!--.*-->"), "")
                ?.trim()
                .orEmpty()
            if (internalConfigs.isBlank()) return

            val jsonArray = JSONArray(internalConfigs)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                if (jsonObject.optString("packageName") == InitFields.hostPackageName) {
                    config = jsonObject.toString()
                    break
                }
            }
        }.onFailure {
            logError("read custom config failed", it)
        }
    }

    private fun startHook(strConfig: String?) {
        val safeConfig = strConfig?.takeIf { it.isNotBlank() } ?: return
        runCatching {
            val appConfig = Json.decodeFromString<AppConfig>(safeConfig)
            val configs = Json.decodeFromString<List<HookConfig>>(appConfig.configs)
            "start custom hook".xLog()
            configs.forEach { hookConfig ->
                if (!hookConfig.enable || hookConfig.isRecordMode()) return@forEach
                when (hookConfig.mode) {
                    Constant.HOOK_STATIC_FIELD -> hookStaticField(hookConfig)
                    Constant.HOOK_FIELD -> hookInstanceField(hookConfig)
                    else -> specificHook(hookConfig)
                }
            }
        }.onFailure {
            logError("start custom hook failed", it)
        }
    }

    private fun specificHook(hookConfig: HookConfig) {
        with(hookConfig) {
            val hooker: Hooker = when (mode) {
                Constant.HOOK_RETURN -> {
                    { hookReturnValue(resultValues, it) }
                }

                Constant.HOOK_RETURN2 -> {
                    { hookReturnObjectValue(resultValues, it, returnClassName) }
                }

                Constant.HOOK_BREAK -> {
                    {}
                }

                Constant.HOOK_PARAM -> {
                    { replaceParamValues(it, resultValues) }
                }

                else -> {
                    throw IllegalStateException("Unsupported hook mode: $mode")
                }
            }

            runCatching {
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
            }.onFailure {
                logError("apply method hook failed: $hookConfig", it)
            }
        }
    }

    private fun hookReturnObjectValue(
        values: String,
        param: XC_MethodHook.MethodHookParam,
        returnClassName: String
    ) {
        try {
            val hookClass = XposedHelpers.findClass(returnClassName, InitFields.ezXClassLoader)
            param.result = gson.fromJson(values, hookClass)
        } catch (_: Throwable) {
            hookReturnValue(values, param)
        }
    }

    private fun hookReturnValue(
        values: String,
        param: XC_MethodHook.MethodHookParam
    ) {
        val targetValue = getDataTypeValue(values)
        if (targetValue is String) {
            if (!applyRandomReturnRule(targetValue, param)) {
                param.result = targetValue
            }
        } else {
            param.result = targetValue
        }
    }

    private fun replaceParamValues(
        param: XC_MethodHook.MethodHookParam,
        values: String
    ) {
        val configuredValues = values.split(",")
        for (i in param.args.indices) {
            val configuredValue = configuredValues.getOrNull(i) ?: continue
            if (configuredValue.isEmpty()) continue
            param.args[i] = getDataTypeValue(configuredValue)
        }
    }

    private fun hookStaticField(hookConfig: HookConfig) {
        with(hookConfig) {
            if (className.isEmpty() || methodName.isEmpty()) {
                runCatching {
                    setStaticFieldValue(fieldClassName, resultValues, fieldName)
                }.onFailure {
                    logError("set static field failed: $hookConfig", it)
                }
                return
            }

            val hooker: Hooker = { setStaticFieldValue(fieldClassName, resultValues, fieldName) }
            hookField(hooker)
        }
    }

    private fun setStaticFieldValue(
        fieldClassName: String,
        values: String,
        fieldName: String
    ) {
        val clazz = XposedHelpers.findClass(fieldClassName, InitFields.ezXClassLoader)
        XposedHelpers.setStaticObjectField(clazz, fieldName, getDataTypeValue(values))
    }

    private fun hookInstanceField(hookConfig: HookConfig) {
        with(hookConfig) {
            val hooker: Hooker = { setInstanceFieldValue(it, resultValues, fieldName) }
            hookField(hooker)
        }
    }

    private fun setInstanceFieldValue(
        param: XC_MethodHook.MethodHookParam,
        values: String,
        fieldName: String
    ) {
        XposedHelpers.setObjectField(param.thisObject, fieldName, getDataTypeValue(values))
    }

    private fun HookConfig.hookField(hooker: Hooker) {
        val isBeforeHook = hookPoint == "before"
        runCatching {
            if (methodName == "*") {
                findAllMethods(className) {
                    true
                }.hook(isBeforeHook, hooker)
            } else if (params == "*") {
                if (methodName == "<init>") {
                    findAllConstructors(className) {
                        true
                    }.hook(isBeforeHook, hooker)
                } else {
                    findAllMethods(className) {
                        name == methodName
                    }.hook(isBeforeHook, hooker)
                }
            } else {
                if (methodName == "<init>") {
                    findConstructor(className) {
                        isSearchConstructor(params)
                    }.hook(isBeforeHook, hooker)
                } else {
                    findMethod(className) {
                        name == methodName && isSearchMethod(params)
                    }.hook(isBeforeHook, hooker)
                }
            }
        }.onFailure {
            logError("apply field hook failed: $this", it)
        }
    }

    private fun applyRandomReturnRule(
        targetValue: String,
        param: XC_MethodHook.MethodHookParam
    ): Boolean {
        val jsonObject = try {
            JSONObject(targetValue)
        } catch (_: Exception) {
            return false
        }

        if (!jsonObject.has("random") || !jsonObject.has("length") || !jsonObject.has("key")) {
            return false
        }

        val randomSeed = jsonObject.optString("random", "a1b2c3d4e5f6g7h8i9k0l")
        val len = jsonObject.optInt("length", 10)
        val updateTime = jsonObject.optLong("updateTime", -1L)
        val key = jsonObject.getString("key")
        val defaultValue = jsonObject.optString("defaultValue")

        fun generateRandomValue(): String = randomSeed.random(len)

        if (updateTime == -1L) {
            param.result = generateRandomValue()
            return true
        }

        val sp = AndroidAppHelper.currentApplication()
            .getSharedPreferences("me.simpleHook", Context.MODE_PRIVATE)
        val timeKey = "time_$key"
        val randomKey = "random_$key"
        val lastUpdateTime = sp.getLong(timeKey, 0L)
        val lastRandomValue = sp.getString(randomKey, defaultValue)
        val currentTime = System.currentTimeMillis() / 1000
        val shouldRefresh = (currentTime - lastUpdateTime) >= updateTime

        if (shouldRefresh) {
            val newRandomValue = generateRandomValue()
            sp.edit()
                .putString(randomKey, newRandomValue)
                .putLong(timeKey, currentTime)
                .apply()
            param.result = newRandomValue
        } else {
            param.result = lastRandomValue
        }
        return true
    }

    private fun HookConfig.isRecordMode(): Boolean {
        return mode == Constant.HOOK_RECORD_PARAMS ||
            mode == Constant.HOOK_RECORD_RETURN ||
            mode == Constant.HOOK_RECORD_PARAMS_RETURN ||
            mode == Constant.HOOK_RECORD_STATIC_FIELD ||
            mode == Constant.HOOK_RECORD_INSTANCE_FIELD
    }

    private fun logError(message: String, throwable: Throwable) {
        "$message: ${throwable.message.orEmpty()}".xLog()
        XposedBridge.log(throwable)
    }
}
