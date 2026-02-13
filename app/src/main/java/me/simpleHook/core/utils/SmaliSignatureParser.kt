package me.simpleHook.core.utils

import android.util.Log
import me.simpleHook.data.MemberInfo

object SmaliSignatureParser {

    private const val TAG = "SmaliSignatureParser"

    private val primitiveTypeMap = mapOf(
        'V' to "void",
        'Z' to "boolean",
        'B' to "byte",
        'S' to "short",
        'C' to "char",
        'I' to "int",
        'J' to "long",
        'F' to "float",
        'D' to "double"
    )

    private val descriptorTokenRegex = Regex("""^[\[\]LBSIJFDZCV;/,\s]+$""")
    private val methodSignatureRegex = Regex("""L([^;]+);->([^(]+)\(([^)]*)\)(.+)""")
    private val fieldSignatureRegex = Regex("""L([^;]+);->([^:]+):(.+)""")

    fun parse(signature: String): MemberInfo? = runCatching {
        parseInternal(signature.trim())
    }.onFailure {
        Log.e(TAG, "parse failed: $signature", it)
    }.getOrNull()

    fun classDescriptorToJavaOrSelf(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("L") || !trimmed.endsWith(";")) return raw
        return parseSingleType(trimmed, 0)
            ?.takeIf { it.second == trimmed.length }
            ?.first ?: raw
    }

    fun toJavaTypeOrSelf(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return raw
        return parseSingleType(trimmed, 0)
            ?.takeIf { (_, next) -> trimmed.substring(next).isBlank() }
            ?.first ?: raw
    }

    fun toJavaParametersOrSelf(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || !looksLikeDescriptorParams(trimmed)) return raw
        val parsed = parseTypeSequence(trimmed) ?: return raw
        return parsed.joinToString(",")
    }

    private fun parseInternal(raw: String): MemberInfo? {
        if (raw.isEmpty()) return null
        val signature = extractMemberSignature(raw)
            ?.substringBefore(" #")
            ?.trim()
            ?: return null
        return if (signature.contains('(') && signature.contains(')')) {
            parseMethodSignature(signature)
        } else {
            val isStaticField = raw.startsWith("sget") || raw.startsWith("sput")
            parseFieldSignature(signature)?.copy(isStatic = isStaticField)
        }
    }

    private fun extractMemberSignature(raw: String): String? {
        val startIndex = raw.indexOf('L')
        if (startIndex < 0) return null
        val memberPointIndex = raw.indexOf("->", startIndex)
        if (memberPointIndex < 0) return null
        return raw.substring(startIndex).trim()
    }

    private fun parseMethodSignature(signature: String): MemberInfo.MethodInfo? {
        val match = methodSignatureRegex.matchEntire(signature) ?: return null
        val (classPath, methodName, paramsRaw, returnTypeRaw) = match.destructured
        val className = classPath.replace('/', '.')
        val paramTypes = parseTypeSequence(paramsRaw) ?: return null
        val returnType = parseType(returnTypeRaw) ?: return null
        return MemberInfo.MethodInfo(
            className = className,
            methodName = methodName,
            parameters = paramTypes,
            returnType = returnType
        )
    }

    private fun parseFieldSignature(signature: String): MemberInfo.FieldInfo? {
        val match = fieldSignatureRegex.matchEntire(signature) ?: return null
        val (classPath, fieldName, fieldTypeRaw) = match.destructured
        val className = classPath.replace('/', '.')
        val fieldType = parseType(fieldTypeRaw) ?: return null
        return MemberInfo.FieldInfo(className, fieldName, fieldType, isStatic = false)
    }

    private fun looksLikeDescriptorParams(raw: String): Boolean {
        if (raw.contains('/')) return true
        if (raw.contains(';')) return true
        if (raw.startsWith("[")) return true
        if (!descriptorTokenRegex.matches(raw)) return false
        return raw.any { it in primitiveTypeMap.keys && it != 'V' }
    }

    private fun parseTypeSequence(sequence: String): List<String>? {
        if (sequence.isEmpty()) return emptyList()
        val types = mutableListOf<String>()
        var index = 0
        while (index < sequence.length) {
            val c = sequence[index]
            if (c == ',' || c.isWhitespace()) {
                index++
                continue
            }
            val type = parseSingleType(sequence, index) ?: return null
            types.add(type.first)
            index = type.second
        }
        return types
    }

    private fun parseType(typeStr: String): String? {
        val trimmed = typeStr.trim()
        val parsed = parseSingleType(trimmed, 0) ?: return null
        return if (trimmed.substring(parsed.second).isBlank()) parsed.first else null
    }

    private fun parseSingleType(params: String, startIndex: Int): Pair<String, Int>? {
        var i = startIndex
        var arrayDepth = 0
        while (i < params.length && params[i] == '[') {
            arrayDepth++
            i++
        }

        if (i >= params.length) return null

        val typeChar = params[i]
        val nextIndex: Int
        val baseType: String = if (typeChar == 'L') {
            val endIndex = params.indexOf(';', i)
            if (endIndex < 0) return null
            val className = params.substring(i + 1, endIndex).replace('/', '.')
            nextIndex = endIndex + 1
            className
        } else {
            val primitiveType = primitiveTypeMap[typeChar] ?: return null
            nextIndex = i + 1
            primitiveType
        }

        var type = baseType
        repeat(arrayDepth) { type += "[]" }
        return type to nextIndex
    }
}
