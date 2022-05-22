package me.simpleHook.hook


import android.content.Context
import me.simpleHook.util.toByteValue
import me.simpleHook.util.toIntValue
import me.simpleHook.util.toLongValue
import me.simpleHook.util.toShortValue
import java.util.regex.Pattern.matches

object Type {
    private const val BYTE_PATTERN = """^-?[0-9]\d*[b|B]$"""
    private const val SHORT_PATTERN = """^-?[0-9]\d*short$"""
    private const val INT_PATTERN = """^-?[0-9]\d*$"""
    private const val LONG_PATTERN = """^-?[0-9]\d*[l|L]$"""
    private const val FLOAT_PATTERN = """^-?[0-9]+.?[0-9]*f$"""
    private const val DOUBLE_PATTERN = """^-?[0-9]+.?[0-9]*d$"""
    private const val BOOLEAN_PATTERN = """(?i)true|false"""
    private const val CHAR_PATTERN = """^.*[c|C]$"""

    private const val STRING_PATTERN_NUMBER = """^-?0?[1-9]\d*[l|L]?[s|S]$"""
    private const val STRING_PATTERN_BOOLEAN = """^(?i)trues|falses$"""
    private const val STRING_PATTERN_NULL = """^(?i)null[s|S]$"""
    private const val STRING_EMPTY_PATTERN = """(?i)empty|空"""
    private const val NULL_PATTERN = """(?i)null"""


    fun getDataTypeValue(value: String) = when {
        matches(BOOLEAN_PATTERN, value) -> value.toBoolean()
        matches(INT_PATTERN, value) -> value.toIntValue
        matches(FLOAT_PATTERN, value) -> value.replace("f", "").toFloat()
        matches(DOUBLE_PATTERN, value) -> value.replace("d", "").toDouble()
        matches(LONG_PATTERN, value) -> value.replace(Regex("""[l|L]"""), "").toLongValue
        matches(NULL_PATTERN, value) -> null
        matches(STRING_EMPTY_PATTERN, value) -> ""
        matches(BYTE_PATTERN, value) -> value.replace(Regex("""[b|B]"""), "").toByteValue
        matches(SHORT_PATTERN, value) -> value.replace(Regex("short"), "").toShortValue
        matches(CHAR_PATTERN, value) -> value.replace(Regex("""[b|B]"""), "")[0]
        matches(STRING_PATTERN_NUMBER, value) -> value.replace(Regex("""[s|S]"""), "")
        matches(STRING_PATTERN_BOOLEAN, value) -> value.removeRange(value.length - 1, value.length)
            .lowercase()
        matches(STRING_PATTERN_NULL, value) -> value.replace(Regex("""[s|S]"""), "")
        else -> value
    }


    fun getClassType(className: String) = when (className) {
        "byte", "B", "b" -> Byte::class.java
        "int", "I", "i" -> Int::class.java
        "short", "S", "s" -> Short::class.java
        "long", "J", "j" -> Long::class.java
        "float", "F", "f" -> Float::class.java
        "double", "D", "d" -> Double::class.java
        "boolean", "Z", "z" -> Boolean::class.java
        "char", "c", "C" -> Char::class.java
        "string" -> String::class.java
        "context" -> Context::class.java
        else -> null
    }
}