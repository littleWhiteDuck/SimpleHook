package me.simpleHook.platform.hook.utils

import android.content.Context
import me.simpleHook.core.utils.toByteValue
import me.simpleHook.core.utils.toIntValue
import me.simpleHook.core.utils.toLongValue
import me.simpleHook.core.utils.toShortValue

object HookTypeParser {

    private val byteRegex = Regex("""^-?\d+[bB]$""")
    private val shortRegex = Regex("""^-?\d+short$""", RegexOption.IGNORE_CASE)
    private val intRegex = Regex("""^-?\d+$""")
    private val longRegex = Regex("""^-?\d+[lL]$""")
    private val floatRegex = Regex("""^-?\d+(\.\d*)?[fF]$""")
    private val doubleRegex = Regex("""^-?\d+(\.\d*)?[dD]$""")
    private val booleanRegex = Regex("""^(true|false)$""", RegexOption.IGNORE_CASE)
    private val charRegex = Regex("""^.[cC]$""")

    private val stringNumberRegex = Regex("""^-?\d+[sS]$""")
    private val stringBooleanRegex = Regex("""^(trues|falses)$""", RegexOption.IGNORE_CASE)
    private val stringNullRegex = Regex("""^nulls$""", RegexOption.IGNORE_CASE)
    private val emptyStringRegex = Regex("""^empty$""", RegexOption.IGNORE_CASE)
    private const val STRING_EMPTY_LIST = "empty_list_string"

    fun getDataTypeValue(value: String): Any? {
        val normalized = value.trim()
        if (normalized.isEmpty()) return ""
        val lowercase = normalized.lowercase()
        return when {
            booleanRegex.matches(normalized) -> lowercase.toBoolean()
            intRegex.matches(normalized) -> normalized.toIntValue
            floatRegex.matches(normalized) -> normalized.dropLast(1).toFloat()
            doubleRegex.matches(normalized) -> normalized.dropLast(1).toDouble()
            longRegex.matches(normalized) -> normalized.dropLast(1).toLongValue
            lowercase == "null" -> null
            emptyStringRegex.matches(normalized) -> ""
            byteRegex.matches(normalized) -> normalized.dropLast(1).toByteValue
            shortRegex.matches(normalized) -> normalized.dropLast(5).toShortValue
            charRegex.matches(normalized) -> normalized[0]
            stringNumberRegex.matches(normalized) -> normalized.dropLast(1)
            stringBooleanRegex.matches(normalized) -> lowercase.dropLast(1)
            stringNullRegex.matches(normalized) -> "null"
            lowercase == STRING_EMPTY_LIST -> emptyList<String>()
            else -> normalized
        }
    }

    fun getClassType(className: String) = when (className.trim().lowercase()) {
        "byte", "b" -> Byte::class.javaPrimitiveType
        "int", "i" -> Int::class.javaPrimitiveType
        "short", "s" -> Short::class.javaPrimitiveType
        "long", "j" -> Long::class.javaPrimitiveType
        "float", "f" -> Float::class.javaPrimitiveType
        "double", "d" -> Double::class.javaPrimitiveType
        "boolean", "z" -> Boolean::class.javaPrimitiveType
        "char", "c" -> Char::class.javaPrimitiveType
        "string" -> String::class.java
        "context" -> Context::class.java
        else -> null
    }

    fun getClassTypeName(className: String): String {
        val normalized = className.trim()
        return if (normalized.endsWith("[]") && normalized.contains(".")) {
            "[L${normalized.removeSuffix("[]")};"
        } else if (normalized.endsWith("[]")) {
            "[${normalized.removeSuffix("[]")}"
        } else {
            getClassType(normalized)?.name ?: normalized
        }
    }
}
