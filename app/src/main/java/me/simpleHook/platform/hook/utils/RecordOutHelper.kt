package me.simpleHook.platform.hook.utils

import android.content.Intent
import com.google.gson.Gson
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.serialization.json.Json
import me.simpleHook.BuildConfig
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.utils.GuiseBase64
import me.simpleHook.core.utils.RecordLogger
import me.simpleHook.core.utils.TimeUtil
import me.simpleHook.core.utils.ToolUtil
import me.simpleHook.data.ExtRecordSettings
import me.simpleHook.data.HookConfig
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.data.record.Base64Operation
import me.simpleHook.data.record.Record
import me.simpleHook.data.record.RecordBase64
import me.simpleHook.data.record.RecordClipboard
import me.simpleHook.data.record.RecordDialog
import me.simpleHook.data.record.RecordDialogType
import me.simpleHook.data.record.RecordError
import me.simpleHook.data.record.RecordErrorType
import me.simpleHook.data.record.RecordExit
import me.simpleHook.data.record.RecordField
import me.simpleHook.data.record.RecordFileOpType
import me.simpleHook.data.record.RecordFileOperation
import me.simpleHook.data.record.RecordHmac
import me.simpleHook.data.record.RecordIntent
import me.simpleHook.data.record.RecordIntentExtra
import me.simpleHook.data.record.RecordJson
import me.simpleHook.data.record.RecordJsonType
import me.simpleHook.data.record.RecordMac
import me.simpleHook.data.record.RecordParam
import me.simpleHook.data.record.RecordParamReturn
import me.simpleHook.data.record.RecordPopupWindow
import me.simpleHook.data.record.RecordPopupWindowType
import me.simpleHook.data.record.RecordReturn
import me.simpleHook.data.record.RecordSignature
import me.simpleHook.data.record.RecordSource
import me.simpleHook.data.record.RecordToast
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordValueType
import java.io.ByteArrayOutputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.zip.GZIPOutputStream


object RecordOutHelper {
    private val gson by lazy(LazyThreadSafetyMode.NONE) { Gson() }
    private val defaultRecordSettings = ExtRecordSettings()
    private val stackTraceFilterPrefixes = listOf(
        "me.simpleHook.platform.hook",
        "com.github.kyuubiran.ezxhelper",
        "io.github.qauxv"
    )

    @Volatile
    private var enableStack: Boolean = defaultRecordSettings.enableStack

    @Volatile
    private var enableBase64: Boolean = defaultRecordSettings.enableBase64

    @Volatile
    private var enableHex: Boolean = defaultRecordSettings.enableHex

    fun applyRecordSettings(settings: ExtRecordSettings?) {
        val safeSettings = settings ?: defaultRecordSettings
        enableStack = safeSettings.enableStack
        enableBase64 = safeSettings.enableBase64
        enableHex = safeSettings.enableHex
        RecordLogger.applyRecordSettings(HookHelper.hostPackageName, safeSettings)
    }

    fun outputError(
        throwable: Throwable,
        hookConfig: HookConfig?,
        supplement: String? = null,
        source: RecordSource = RecordSource.Custom
    ) {
        val type = when (throwable) {
            is NoSuchMethodException, is NoSuchMethodError -> RecordErrorType.Method
            is NoSuchFieldException, is NoSuchFieldError -> RecordErrorType.Field
            is ClassNotFoundException, is XposedHelpers.ClassNotFoundError -> RecordErrorType.Class
            else -> RecordErrorType.Other
        }
        val errorRecord = RecordError(
            errorType = type,
            hookConfig = hookConfig,
            supplement = supplement,
            stackDetail = formatThrowableStack(throwable)
        )
        outputRecord(type = RecordType.Error, record = errorRecord, source = source)
    }

    data class MethodSignature(
        val className: String,
        val methodName: String,
        val params: List<String>
    )

    fun buildMethodSignature(member: Member): MethodSignature {
        val className = member.declaringClass.name.orEmpty()
        val methodName = if (member is Constructor<*>) "<init>" else member.name
        return MethodSignature(
            className = className,
            methodName = methodName,
            params = member.toMethodParams()
        )
    }

    fun outputFieldRecord(
        fieldValue: Any?,
        hookConfig: HookConfig,
        signature: MethodSignature? = null,
        source: RecordSource = RecordSource.Custom
    ) {
        val pureStatic = hookConfig.className.isEmpty() || hookConfig.methodName.isEmpty()
        val instanceHook =
            hookConfig.mode == Constant.HOOK_RECORD_INSTANCE_FIELD || hookConfig.mode == Constant.HOOK_FIELD
        val className = signature?.className ?: hookConfig.className
        val methodName = signature?.methodName ?: hookConfig.methodName
        val params = signature?.params ?: emptyList()

        val fieldRecord = RecordField(
            className = className.takeIf { !pureStatic },
            methodName = methodName.takeIf { !pureStatic },
            fieldClassName = hookConfig.fieldClassName.takeIf { !instanceHook },
            params = emptyList<String>().takeIf { pureStatic } ?: params,
            fieldName = hookConfig.fieldName,
            filedValue = fieldValue.recordValue
        )

        outputRecord(type = RecordType.RecordField, record = fieldRecord, source = source)
    }

    fun outputParamRecord(
        paramValues: Array<out Any?>,
        signature: MethodSignature,
        source: RecordSource = RecordSource.Custom
    ) {
        val paramRecord = RecordParam(
            className = signature.className,
            methodName = signature.methodName,
            params = signature.params,
            paramValues = paramValues.map { it.recordValue },
            callStack = getStackTrace(ignoreSwitch = true)
        )
        outputRecord(type = RecordType.RecordParam, record = paramRecord, source = source)
    }

    fun outputParamReturnRecord(
        paramValues: Array<out Any?>,
        returnValue: Any?,
        signature: MethodSignature,
        source: RecordSource = RecordSource.Custom
    ) {
        val paramReturnRecord = RecordParamReturn(
            className = signature.className,
            methodName = signature.methodName,
            params = signature.params,
            paramValues = paramValues.map { it.recordValue },
            returnValue = returnValue.recordValue,
            callStack = getStackTrace(ignoreSwitch = true)
        )
        outputRecord(type = RecordType.RecordParamReturn, record = paramReturnRecord, source = source)
    }


    fun outputReturnRecord(
        returnValue: Any?,
        signature: MethodSignature,
        source: RecordSource = RecordSource.Custom
    ) {
        val returnRecord = RecordReturn(
            className = signature.className,
            methodName = signature.methodName,
            params = signature.params,
            returnValue = returnValue.recordValue,
            callStack = getStackTrace(ignoreSwitch = true)
        )
        outputRecord(type = RecordType.RecordReturn, record = returnRecord, source = source)
    }

    private fun Member.toMethodParams(): List<String> {
        return when (this) {
            is Method -> parameterTypes.map { it.displayName() }
            is Constructor<*> -> parameterTypes.map { it.displayName() }
            else -> emptyList()
        }
    }

    private fun Class<*>.displayName(): String {
        return canonicalName ?: name
    }


    fun outputRecord(
        type: RecordType,
        record: Record,
        subType: String? = null,
        source: RecordSource = RecordSource.Extension
    ) {
        val recordContent = Json.encodeToString(
            RecordEntity(
                type = type,
                subType = subType ?: type.name,
                record = Json.encodeToString(record),
                packageName = HookHelper.hostPackageName,
                processName = HookHelper.hostProcessName,
                time = TimeUtil.getCurrentTime(pattern = "yy-MM-dd HH:mm:ss")
            )
        )
//


        val compressedRecordContent = compressToBase64(recordContent) ?: return
        RecordLogger.write(
            packageName = HookHelper.hostPackageName,
            content = compressedRecordContent,
            source = source
        )
    }

    fun compress(text: String): ByteArray? {
        return try {
            ByteArrayOutputStream().use { byteOut ->
                GZIPOutputStream(byteOut).use { gzipOut ->
                    gzipOut.write(text.toByteArray(Charsets.UTF_8))
                }
                byteOut.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressToBase64(text: String): String? {
        return compress(text)?.let { compressedBytes ->
            GuiseBase64.encodeToString(compressedBytes, GuiseBase64.NO_WRAP)
        }
    }

    fun outputBase64(operation: Base64Operation, rawData: ByteArray, resultData: ByteArray) {
        val base64Record = RecordBase64(
            operation = operation,
            rawData = rawData.recordValue,
            resultData = resultData.recordValue,
            stackDetail = getStackTraceStr()
        )
        outputRecord(type = RecordType.Base64, record = base64Record)
    }

    fun outputClipboard(isRead: Boolean, info: String) {
        outputRecord(
            type = RecordType.Clipboard,
            record = RecordClipboard(
                isRead = isRead,
                info = info,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputExitRecord(type: String) {
        outputRecord(
            type = RecordType.Exit, record = RecordExit(
                exitType = type,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputFileOperation(fileOpType: RecordFileOpType, path: String, partData: String? = null) {
        outputRecord(
            type = RecordType.FileOperation,
            record = RecordFileOperation(
                operation = fileOpType,
                path = path,
                partData = partData,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputSignature(signByteArray: ByteArray) {
        outputRecord(
            type = RecordType.Signature, record = RecordSignature(
                md5 = ToolUtil.getDigest(bytes = signByteArray).takeIf { enableHex },
                sha1 = ToolUtil.getDigest(bytes = signByteArray, "SHA-1").takeIf { enableHex },
                sha256 = ToolUtil.getDigest(bytes = signByteArray, "SHA-256").takeIf { enableHex },
                charStr = signByteArray.toHex(lowercase = true).takeIf { enableHex },
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputJson(type: RecordJsonType, values: Map<String, String>) {
        val jsonRecord = RecordJson(
            jsonType = type,
            values = values,
            stackDetail = getStackTraceStr()
        )
        outputRecord(type = RecordType.Json, record = jsonRecord)
    }

    fun outputDialog(type: RecordDialogType, textList: List<String>) {
        outputRecord(
            type = RecordType.Dialog, record = RecordDialog(
                dialogType = type,
                textList = textList,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputPopup(type: RecordPopupWindowType, textList: List<String>) {
        outputRecord(
            type = RecordType.PopupWindow, record = RecordPopupWindow(
                popupType = type,
                textList = textList,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputToast(textList: List<String>) {
        outputRecord(
            type = RecordType.Toast, record = RecordToast(
                textList = textList,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputHmac(
        algorithm: String,
        key: Map<RecordValueType, String>?,
        rawData: ByteArray,
        resultData: ByteArray
    ) {
        outputRecord(
            type = RecordType.Hmac, subType = algorithm, record = RecordHmac(
                algorithm = algorithm,
                key = key,
                rawData = rawData.recordValue,
                resultData = resultData.recordValue,
                stackDetail = getStackTraceStr()
            )
        )
    }

    fun outputMac(
        algorithm: String, rawData: ByteArray, resultData: ByteArray

    ) {
        outputRecord(
            type = RecordType.Mac,
            subType = algorithm,
            record = RecordMac(
                algorithm = algorithm,
                rawData = rawData.recordValue,
                resultData = resultData.recordValue,
                stackDetail = getStackTraceStr()
            )
        )
    }

    @Suppress("DEPRECATION")
    fun outputIntent(intent: Intent) {
        val extras = intent.extras?.keySet()?.map {
            RecordIntentExtra(
                intentType = intent.extras!!.get(it)?.javaClass?.name ?: "unknown",
                key = it,
                intent.extras!!.get(it).recordValue
            )
        } ?: emptyList()
        val intentRecord = RecordIntent(
            packageName = intent.component?.packageName ?: "",
            className = intent.component?.className ?: "",
            action = intent.action ?: "",
            data = intent.dataString ?: "",
            extras = extras
        )
        outputRecord(type = RecordType.Intent, record = intentRecord)
    }


    fun getStackTraceStr(): String {
        if (!enableStack) return ""
        return getStackTrace().joinToString("\n")
    }

    private fun getStackTrace(ignoreSwitch: Boolean = false): List<String> {
        if (!ignoreSwitch && !enableStack) return emptyList()
        val stackElements = Throwable().stackTrace.toList()
        val startIndex = stackElements.indexOfLast { it.className == "LSPHooker_" }
        return stackElements
            .subList(startIndex.takeIf { it != -1 } ?: 0, stackElements.size)
            .filterNot(::shouldFilterStackElement)
            .map(::formatStackElement)
    }

    private fun shouldFilterStackElement(element: StackTraceElement): Boolean {
        if (BuildConfig.DEBUG) return false
        return stackTraceFilterPrefixes.any { prefix ->
            element.className == prefix || element.className.startsWith("$prefix.")
        }
    }

    private fun formatStackElement(element: StackTraceElement): String {
        return "${element.className} --> ${element.methodName}(line:${element.lineNumber})"
    }

    private fun formatThrowableStack(throwable: Throwable): String {
        val filteredLines = throwable.stackTrace
            .filterNot(::shouldFilterStackElement)
            .map(::formatStackElement)
        return buildString {
            appendLine(throwable.toString())
            if (filteredLines.isNotEmpty()) {
                append(filteredLines.joinToString("\n"))
            }
        }.trimEnd()
    }

    private fun Any?.toGsonStringSafe(): String {
        return try {
            gson.toJson(this)
        } catch (t: Throwable) {
            val className = this?.javaClass?.name ?: "null"
            "$className (Gson error: ${t.javaClass.simpleName})"
        }
    }

    private val Any?.recordValue: Map<RecordValueType, String>
        get() {
            if (this@recordValue is ByteArray) return this@recordValue.recordValue
            return buildMap {
                put(RecordValueType.ToString, this@recordValue.toString())
                put(RecordValueType.GsonToString, this@recordValue.toGsonStringSafe())
            }
        }

    val ByteArray.recordValue: Map<RecordValueType, String>
        get() {
            return buildMap {
                put(RecordValueType.BytesToString, String(this@recordValue))
                if (enableBase64) {
                    put(
                        RecordValueType.Base64, GuiseBase64.encodeToString(
                            this@recordValue,
                            GuiseBase64.DEFAULT
                        )
                    )
                }
                if (enableHex) {
                    put(RecordValueType.Hex, this@recordValue.toHex())
                }
            }
        }

    fun ByteArray.toHex(lowercase: Boolean = false): String {
        val hexChars = if (lowercase) {
            "0123456789abcdef"
        } else {
            "0123456789ABCDEF"
        }.toCharArray()

        val hexBuilder = StringBuilder(size * 2)
        for (b in this) {
            val unsignedByte = b.toInt() and 0xFF
            val highNibble = unsignedByte / 16
            val lowNibble = unsignedByte % 16
            hexBuilder.append(hexChars[highNibble])
            hexBuilder.append(hexChars[lowNibble])
        }
        return hexBuilder.toString()
    }


    fun writeWithCap(
        stream: ByteArrayOutputStream,
        data: ByteArray,
        off: Int = 0,
        len: Int = data.size
    ) {
        // 1MB
        val remain = 1024 * 1024 - stream.size()
        if (remain <= 0) return
        val toWrite = kotlin.math.min(len, remain)
        try {
            stream.write(data, off, toWrite)
        } catch (_: Throwable) {

        }
    }
}
