package me.simpleHook.platform.hook.extension

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import me.simpleHook.data.ExtensionConfig
import me.simpleHook.data.record.RecordJsonType

import me.simpleHook.platform.hook.utils.HookUtils.toDisplayString
import me.simpleHook.platform.hook.utils.RecordOutHelper
import org.json.JSONArray
import org.json.JSONObject

object JSONHook : BaseHook() {

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.jsonConfig.recordObject) {
            hookJSONObject()
        }
        if (extensionConfig.jsonConfig.recordArray) {
            hookJSONArray()
        }
    }

    private fun hookJSONObject() {
        XposedBridge.hookAllMethods(JSONObject::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val name = param.args[0] as String
                val value = toDisplayString(param.args[1])
                RecordOutHelper.outputJson(
                    type = RecordJsonType.JsonObjectPut,
                    values = mapOf(name to value)
                )
            }
        })

        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // 不准确
                @Suppress("UNCHECKED_CAST")
                val map = XposedHelpers.getObjectField(
                    param.thisObject,
                    "nameValuePairs"
                ) as LinkedHashMap<String, Any>
                if (map.isEmpty()) return
                RecordOutHelper.outputJson(
                    type = RecordJsonType.JsonObjectCreate,
                    values = map.mapValues { toDisplayString(it.value) }
                )
            }
        })
    }

    private fun hookJSONArray() {

        XposedBridge.hookAllMethods(JSONArray::class.java, "put", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val (name, value) = when (param.args.size) {
                    1 -> {
                        // put(value): append style
                        "append" to toDisplayString(param.args[0])
                    }

                    2 -> {
                        // put(index, value)
                        param.args[0].toString() to toDisplayString(param.args[1])
                    }

                    else -> {
                        return
                    }
                }
                RecordOutHelper.outputJson(
                    type = RecordJsonType.JsonArrayPut,
                    values = mapOf(name to value)
                )
            }
        })

        XposedBridge.hookAllConstructors(JSONArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val jsonObject = param.thisObject

                @Suppress("UNCHECKED_CAST")
                val value = XposedHelpers.getObjectField(jsonObject, "values") as List<Any>
                RecordOutHelper.outputJson(
                    type = RecordJsonType.JsonArrayCreate,
                    values = mapOf("JSON_ARRAY" to toDisplayString(value))
                )
            }
        })
    }

}
