package me.simpleHook.platform.hook

import android.app.AndroidAppHelper
import android.content.Context
import androidx.core.content.edit
import com.github.kyuubiran.ezxhelper.utils.Hooker
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorBefore
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.showToast
import com.google.gson.Gson
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.serialization.json.Json
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.HookConfig
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.core.extension.random
import me.simpleHook.platform.hook.extension.ADBHook
import me.simpleHook.platform.hook.extension.ApplicationHook
import me.simpleHook.platform.hook.extension.Base64Hook
import me.simpleHook.platform.hook.extension.BaseHook
import me.simpleHook.platform.hook.extension.CipherHook
import me.simpleHook.platform.hook.extension.ClickEventHook
import me.simpleHook.platform.hook.extension.ClipboardHook
import me.simpleHook.platform.hook.extension.ContactHook
import me.simpleHook.platform.hook.extension.DialogHook
import me.simpleHook.platform.hook.extension.ExitHook
import me.simpleHook.platform.hook.extension.FileHook
import me.simpleHook.platform.hook.extension.HmacHook
import me.simpleHook.platform.hook.extension.HotFixHook
import me.simpleHook.platform.hook.extension.IntentHook
import me.simpleHook.platform.hook.extension.JSONHook
import me.simpleHook.platform.hook.extension.MessageDigestHook
import me.simpleHook.platform.hook.extension.PopupWindowHook
import me.simpleHook.platform.hook.extension.SensorMangerHook
import me.simpleHook.platform.hook.extension.SignatureHook
import me.simpleHook.platform.hook.extension.ToastHook
import me.simpleHook.platform.hook.extension.VpnCheckHook
import me.simpleHook.platform.hook.extension.WebHook
import me.simpleHook.platform.hook.utils.HookHelper
import me.simpleHook.platform.hook.utils.HookHelper.appContext
import me.simpleHook.platform.hook.utils.RecordOutHelper
import me.simpleHook.platform.hook.utils.Type.getDataTypeValue
import me.simpleHook.platform.hook.utils.hook
import me.simpleHook.platform.hook.utils.isSearchConstructor
import me.simpleHook.platform.hook.utils.isSearchMethod
import me.simpleHook.platform.hook.utils.xLog
import org.json.JSONObject


object MainHook {

    fun readyHook(strConfig: String) {
        if (strConfig.isBlank()) return
        try {
            val appConfig = Json.decodeFromString<AppConfig>(strConfig)
            if (!appConfig.enable) return
            val configs = Json.decodeFromString<List<HookConfig>>(appConfig.configs)
            "start custom hook".xLog()
            configs.forEach { hookConfig ->
                if (!hookConfig.enable) return@forEach
                hookConfig.apply {
                    when (hookConfig.mode) {
                        Constant.HOOK_STATIC_FIELD, Constant.HOOK_RECORD_STATIC_FIELD -> {
                            FieldHook.hookStaticField(hookConfig)
                        }

                        Constant.HOOK_FIELD, Constant.HOOK_RECORD_INSTANCE_FIELD -> {
                            FieldHook.hookInstanceField(hookConfig)
                        }

                        else -> methodHook(hookConfig)
                    }
                }
            }
        } catch (e: Throwable) {
            RecordOutHelper.outputError(throwable = e, hookConfig = null, supplement = strConfig)
        }
    }


    private fun methodHook(hookConfig: HookConfig) {
        with(hookConfig) {
            val hooker: Hooker = when (mode) {
                Constant.HOOK_RETURN -> {
                    { hookReturnValue(resultValues, it) }
                }

                Constant.HOOK_RETURN2 -> {
                    { hookReturnValuePro(resultValues, it, returnClassName) }
                }

                Constant.HOOK_BREAK -> {
                    {}
                }

                Constant.HOOK_PARAM -> {
                    { hookParamsValue(param = it, paramValues = resultValues) }
                }

                Constant.HOOK_RECORD_PARAMS -> {
                    { recordParamsValue(param = it, hookConfig = hookConfig) }
                }

                Constant.HOOK_RECORD_RETURN -> {
                    { recordReturnValue(param = it, hookConfig = hookConfig) }
                }

                Constant.HOOK_RECORD_PARAMS_RETURN -> {
                    { recordParamsAndReturn(param = it, hookConfig = hookConfig) }
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
                RecordOutHelper.outputError(throwable = e, hookConfig = hookConfig)
            }
        }
    }

    private fun hookReturnValuePro(
        values: String, param: XC_MethodHook.MethodHookParam, returnClassName: String
    ) {
        val hookClass = XposedHelpers.findClass(returnClassName, HookHelper.appClassLoader)
        try {
            val hookObject = Gson().fromJson(values, hookClass)
            param.result = hookObject
        } catch (_: Exception) {
            hookReturnValue(values, param)
        }
    }

    private fun hookReturnValue(
        values: String, param: XC_MethodHook.MethodHookParam
    ) {
        val targetValue = getDataTypeValue(values)
        if (targetValue is String) {
            try {
                randomReturn(targetValue, param)
            } catch (_: Exception) {
                param.result = targetValue
            }
        } else {
            param.result = targetValue
        }
    }



    private fun hookParamsValue(
        param: XC_MethodHook.MethodHookParam,
        paramValues: String
    ) {
        for (i in param.args.indices) {
            if (paramValues.split(",")[i] == "") continue
            val targetValue = getDataTypeValue(paramValues.split(",")[i])
            param.args[i] = targetValue
        }
    }

    private fun recordParamsValue(
        param: XC_MethodHook.MethodHookParam,
        hookConfig: HookConfig
    ) {
        RecordOutHelper.outputParamRecord(paramValues = param.args, hookConfig = hookConfig)
    }

    private fun recordReturnValue(param: XC_MethodHook.MethodHookParam, hookConfig: HookConfig) {
        RecordOutHelper.outputReturnRecord(returnValue = param.result, hookConfig = hookConfig)
    }

    private fun recordParamsAndReturn(
        param: XC_MethodHook.MethodHookParam, hookConfig: HookConfig
    ) {
        RecordOutHelper.outputParamReturnRecord(
            returnValue = param.result,
            paramValues = param.args,
            hookConfig = hookConfig
        )
    }


    fun readyExtensionHook(
        strConfig: String
    ) {
        try {
            if (strConfig.trim().isEmpty()) return
            "start extension hook".xLog()
            val extensionConfig = Json.decodeFromString<ExtensionConfig>(strConfig)
            RecordOutHelper.applyRecordSettings(extensionConfig.recordSettings)
            if (!extensionConfig.all) return
            if (extensionConfig.hookTip) appContext.showToast(msg = "SimpleHook: StartHook")
            initExtensionHook(
                extensionConfig = extensionConfig,
                DialogHook,
                PopupWindowHook,
                ToastHook,
                HotFixHook,
                IntentHook,
                ClickEventHook,
                VpnCheckHook,
                Base64Hook,
                MessageDigestHook,
                HmacHook,
                CipherHook,
                JSONHook,
                WebHook,
                ClipboardHook,
                ApplicationHook,
                SignatureHook,
                ContactHook,
                SensorMangerHook,
                ADBHook,
                FileHook,
                ExitHook
            )
        } catch (e: Throwable) {
            RecordOutHelper.outputError(throwable = e, supplement = strConfig, hookConfig = null)
        }
    }

    private fun initExtensionHook(
        extensionConfig: ExtensionConfig, vararg hooks: BaseHook
    ) {
        hooks.forEach {
            if (it.isInit) return@forEach
            it.isInit = true
            it.startHook(extensionConfig)
        }
    }


    private fun randomReturn(
        targetValue: String,
        param: XC_MethodHook.MethodHookParam
    ) {
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
                    sp.edit { putString("random_$key", result) }
                    sp.edit { putLong("time_$key", currentTime) }
                    param.result = result
                } else {
                    param.result = oldRandom
                }
            }
        }
    }

}


