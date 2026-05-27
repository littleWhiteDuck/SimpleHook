package me.simpleHook.core.utils

import kotlinx.serialization.json.Json
import me.simpleHook.core.constant.Constant.HOOK_BREAK
import me.simpleHook.core.constant.Constant.HOOK_FIELD
import me.simpleHook.core.constant.Constant.HOOK_PARAM
import me.simpleHook.core.constant.Constant.HOOK_RECORD_INSTANCE_FIELD
import me.simpleHook.core.constant.Constant.HOOK_RECORD_PARAMS
import me.simpleHook.core.constant.Constant.HOOK_RECORD_PARAMS_RETURN
import me.simpleHook.core.constant.Constant.HOOK_RECORD_RETURN
import me.simpleHook.core.constant.Constant.HOOK_RECORD_STATIC_FIELD
import me.simpleHook.core.constant.Constant.HOOK_RETURN
import me.simpleHook.core.constant.Constant.HOOK_RETURN2
import me.simpleHook.core.constant.Constant.HOOK_STATIC_FIELD
import me.simpleHook.data.AppConfigItem2
import me.simpleHook.data.HookConfig
import me.simpleHook.platform.hook.utils.HookTypeParser

object FridaHook {

    fun getStringFridaConfig(list: List<AppConfigItem2>?) = list?.let {
        buildString {
            appendLine("'use strict';")
            appendLine()
            appendLine("Java.perform(function () {")
            appendLine("    const ActivityThread = Java.use('android.app.ActivityThread');")
            appendLine("    const hostPackageName = String(ActivityThread.currentPackageName());")
            appendLine()
            appendLine("    function simpleHookObjectReturn(className, jsonText) {")
            appendLine("        try {")
            appendLine("            const Gson = Java.use('com.google.gson.Gson');")
            appendLine("            const TargetClass = Java.use(className).class;")
            appendLine("            return Gson.\$new().fromJson(jsonText, TargetClass);")
            appendLine("        } catch (e) {")
            appendLine("            console.warn('[SimpleHook] Object return from JSON requires Gson in host app or manual Frida mapping: ' + e);")
            appendLine("            return null;")
            appendLine("        }")
            appendLine("    }")
            appendLine()
            list.forEach { configItem ->
                val appConfig = configItem.appConfig
                val configStr = toFridaConfig(appConfig.configs, "        ")
                appendLine("    if (hostPackageName === ${jsString(appConfig.packageName)}) {")
                if (appConfig.description.isNotBlank()) {
                    appendLine("        // ${sanitizeComment(appConfig.description)}")
                }
                append(configStr)
                appendLine("    }")
                appendLine()
            }
            appendLine("});")
        }
    } ?: ""

    private fun toFridaConfig(configStr: String, indent: String): String {
        val configs = Json.decodeFromString<List<HookConfig>>(configStr)
        return buildString {
            configs.forEachIndexed { index, config ->
                if (config.desc.isNotBlank()) {
                    appendLine("$indent// ${sanitizeComment(config.desc)}")
                }
                append(toFridaSnippet(config, index, indent))
                appendLine()
            }
        }
    }

    private fun toFridaSnippet(config: HookConfig, index: Int, indent: String): String {
        if (config.mode == HOOK_STATIC_FIELD && !config.hasMethodTarget()) {
            return staticFieldSnippet(config, index, indent, isRecord = false)
        }
        if (config.mode == HOOK_RECORD_STATIC_FIELD && !config.hasMethodTarget()) {
            return staticFieldSnippet(config, index, indent, isRecord = true)
        }
        if (!config.hasMethodTarget()) {
            return "$indent// Skipped: missing target class or method for mode ${config.mode}.\n"
        }
        return methodHookSnippet(config, index, indent)
    }

    private fun staticFieldSnippet(
        config: HookConfig,
        index: Int,
        indent: String,
        isRecord: Boolean
    ): String {
        val fieldClassName = config.staticFieldClassName()
        if (fieldClassName.isBlank() || config.fieldName.isBlank()) {
            return "$indent// Skipped: missing static field class or field name.\n"
        }
        val fieldClassVar = "FieldClass$index"
        val field = fieldAccess(fieldClassVar, config.fieldName)
        return buildString {
            appendLine("${indent}const $fieldClassVar = Java.use(${jsString(fieldClassName)});")
            if (isRecord) {
                appendLine("${indent}console.log(${jsString("${config.fieldName}: ")} + $field);")
            } else {
                appendLine("${indent}$field = ${configValue(config.resultValues)};")
            }
        }
    }

    private fun methodHookSnippet(config: HookConfig, index: Int, indent: String): String {
        val targetVar = "Target$index"
        val hookVar = "Hook$index"
        val fieldClassVar = "FieldClass$index"
        val usesStaticField = config.mode == HOOK_STATIC_FIELD || config.mode == HOOK_RECORD_STATIC_FIELD
        val needsStaticFieldClass = usesStaticField && config.staticFieldClassName().isNotBlank()
        return buildString {
            appendLine("${indent}const $targetVar = Java.use(${jsString(config.className)});")
            if (needsStaticFieldClass) {
                appendLine("${indent}const $fieldClassVar = Java.use(${jsString(config.staticFieldClassName())});")
            }
            if (config.params.trim() == "*") {
                appendAllOverloadsHook(config, targetVar, fieldClassVar, indent)
            } else {
                appendFixedOverloadHook(config, targetVar, hookVar, fieldClassVar, indent)
            }
        }
    }

    private fun StringBuilder.appendFixedOverloadHook(
        config: HookConfig,
        targetVar: String,
        hookVar: String,
        fieldClassVar: String,
        indent: String
    ) {
        val parameterTypes = parameterTypes(config.params)
        val argNames = parameterTypes.indices.map { "arg$it" }
        val actions = hookActions(config, argNames, fieldClassVar, allOverloads = false)
        val overloadArgs = parameterTypes.joinToString(", ") { jsString(it) }
        val functionArgs = argNames.joinToString(", ")
        val callArgs = argNames.joinToString(", ")
        appendLine("${indent}const $hookVar = ${methodAccess(targetVar, config.methodName)}.overload($overloadArgs);")
        appendLine("${indent}$hookVar.implementation = function ($functionArgs) {")
        actions.beforeLines.forEach { appendLine("$indent    $it") }
        if (actions.returnValue != null) {
            appendLine("$indent    return ${actions.returnValue};")
        } else {
            val originalCall = if (callArgs.isBlank()) {
                "$hookVar.call(this)"
            } else {
                "$hookVar.call(this, $callArgs)"
            }
            appendLine("${indent}    const result = $originalCall;")
            actions.afterLines.forEach { appendLine("$indent    $it") }
            appendLine("${indent}    return result;")
        }
        appendLine("${indent}};")
    }

    private fun StringBuilder.appendAllOverloadsHook(
        config: HookConfig,
        targetVar: String,
        fieldClassVar: String,
        indent: String
    ) {
        val actions = hookActions(config, emptyList(), fieldClassVar, allOverloads = true)
        appendLine("${indent}${methodAccess(targetVar, config.methodName)}.overloads.forEach(function (overload) {")
        appendLine("${indent}    overload.implementation = function () {")
        appendLine("${indent}        const args = Array.prototype.slice.call(arguments);")
        actions.beforeLines.forEach { appendLine("$indent        $it") }
        if (actions.returnValue != null) {
            appendLine("$indent        return ${actions.returnValue};")
        } else {
            appendLine("${indent}        const result = overload.apply(this, args);")
            actions.afterLines.forEach { appendLine("$indent        $it") }
            appendLine("${indent}        return result;")
        }
        appendLine("${indent}    };")
        appendLine("${indent}});")
    }

    private fun hookActions(
        config: HookConfig,
        argNames: List<String>,
        fieldClassVar: String,
        allOverloads: Boolean
    ): HookActions {
        return when (config.mode) {
            HOOK_RETURN -> HookActions(returnValue = configValue(config.resultValues))
            HOOK_RETURN2 -> HookActions(
                returnValue = "simpleHookObjectReturn(${jsString(config.returnClassName)}, ${
                    jsString(
                        config.resultValues
                    )
                })"
            )

            HOOK_BREAK -> HookActions(returnValue = "null")
            HOOK_PARAM -> HookActions(
                beforeLines = paramAssignments(config.resultValues, argNames, allOverloads)
            )

            HOOK_STATIC_FIELD -> fieldHookActions(
                config,
                "${fieldAccess(fieldClassVar, config.fieldName)} = ${configValue(config.resultValues)};"
            )

            HOOK_FIELD -> fieldHookActions(
                config,
                "${fieldAccess("this", config.fieldName)} = ${configValue(config.resultValues)};"
            )

            HOOK_RECORD_RETURN -> HookActions(afterLines = listOf("console.log('return: ' + result);"))
            HOOK_RECORD_PARAMS -> HookActions(afterLines = recordParamLines(argNames, allOverloads))
            HOOK_RECORD_PARAMS_RETURN -> HookActions(
                afterLines = listOf("console.log('return: ' + result);") + recordParamLines(
                    argNames,
                    allOverloads
                )
            )

            HOOK_RECORD_STATIC_FIELD -> fieldHookActions(
                config,
                "console.log(${jsString("${config.fieldName}: ")} + ${fieldAccess(fieldClassVar, config.fieldName)});"
            )

            HOOK_RECORD_INSTANCE_FIELD -> fieldHookActions(
                config,
                "console.log(${jsString("${config.fieldName}: ")} + ${fieldAccess("this", config.fieldName)});"
            )

            else -> HookActions(beforeLines = listOf("// Unsupported SimpleHook mode: ${config.mode}."))
        }
    }

    private fun fieldHookActions(config: HookConfig, line: String): HookActions {
        if (config.fieldName.isBlank()) {
            return HookActions(beforeLines = listOf("// Skipped: missing field name."))
        }
        return if (config.hookPoint == "before") {
            HookActions(beforeLines = listOf(line))
        } else {
            HookActions(afterLines = listOf(line))
        }
    }

    private fun paramAssignments(
        resultValues: String,
        argNames: List<String>,
        allOverloads: Boolean
    ): List<String> {
        return resultValues.split(",").mapIndexedNotNull { index, rawValue ->
            if (rawValue.isBlank()) return@mapIndexedNotNull null
            val target = if (allOverloads) {
                "args[$index]"
            } else {
                argNames.getOrNull(index) ?: return@mapIndexedNotNull null
            }
            "$target = ${configValue(rawValue)};"
        }
    }

    private fun recordParamLines(argNames: List<String>, allOverloads: Boolean): List<String> {
        if (allOverloads) {
            return listOf("args.forEach(function (value, index) { console.log('arg' + index + ': ' + value); });")
        }
        return argNames.mapIndexed { index, argName -> "console.log('arg$index: ' + $argName);" }
    }

    private fun configValue(value: String): String {
        return jsValue(HookTypeParser.getDataTypeValue(value))
    }

    private fun jsValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> jsString(value)
            is Char -> jsString(value.toString())
            is Boolean -> value.toString()
            is Number -> value.toString()
            is List<*> -> "[]"
            else -> jsString(value.toString())
        }
    }

    private fun parameterTypes(params: String): List<String> {
        val normalized = params.trim()
        if (normalized.isEmpty() || normalized == "*") return emptyList()
        return normalized.split(",").map { HookTypeParser.getClassTypeName(it.trim()) }
            .filter { it.isNotBlank() }
    }

    private fun methodAccess(receiver: String, methodName: String): String {
        if (methodName == "<init>") return "$receiver.\$init"
        return memberAccess(receiver, methodName)
    }

    private fun fieldAccess(receiver: String, fieldName: String): String {
        return "${memberAccess(receiver, fieldName)}.value"
    }

    private fun memberAccess(receiver: String, name: String): String {
        return if (name.matches(Regex("""[A-Za-z_$][A-Za-z0-9_$]*"""))) {
            "$receiver.$name"
        } else {
            "$receiver[${jsString(name)}]"
        }
    }

    private fun jsString(value: String): String {
        return buildString {
            append("'")
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '\'' -> append("\\'")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
            append("'")
        }
    }

    private fun sanitizeComment(value: String): String {
        return value.replace("\r", " ").replace("\n", " ")
    }

    private fun HookConfig.hasMethodTarget(): Boolean {
        return className.isNotBlank() && methodName.isNotBlank()
    }

    private fun HookConfig.staticFieldClassName(): String {
        return fieldClassName.ifBlank { className }
    }

    private data class HookActions(
        val beforeLines: List<String> = emptyList(),
        val afterLines: List<String> = emptyList(),
        val returnValue: String? = null
    )
}
