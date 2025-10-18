package me.simpleHook.hook.utils

import android.content.Intent
import com.google.gson.Gson
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.serialization.json.Json
import me.simpleHook.constant.ConfigConstant
import me.simpleHook.data.HookConfig
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
import me.simpleHook.data.record.RecordToast
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.database.entity.RecordEntity
import me.simpleHook.utils.GuiseBase64
import me.simpleHook.utils.RecordLogger
import me.simpleHook.utils.TimeUtil
import me.simpleHook.utils.ToolUtil
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset


object RecordOutHelper {
    private val recordPath = String.format(ConfigConstant.RECORD_PATH, HookHelper.hostPackageName)

    fun outputError(throwable: Throwable, hookConfig: HookConfig?, supplement: String? = null) {
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
            stackDetail = throwable.stackTraceToString()
        )
        outputRecord(type = RecordType.Error, record = errorRecord)
    }

    fun outputFieldRecord(filedValue: Any?, hookConfig: HookConfig) {
        val fieldRecord = RecordField(
            className = hookConfig.className,
            methodName = hookConfig.methodName,
            fieldClassName = hookConfig.fieldClassName,
            params = hookConfig.params.split(","),
            fieldName = hookConfig.fieldName,
            filedValue = filedValue.recordValue
        )

        outputRecord(type = RecordType.RecordFiled, record = fieldRecord)
    }

    fun outputParamRecord(paramValues: Array<out Any?>, hookConfig: HookConfig) {
        val paramRecord = RecordParam(
            className = hookConfig.className,
            methodName = hookConfig.methodName,
            params = hookConfig.params.split(","),
            paramValues = paramValues.map { it.recordValue },
            callStack = getStackTrace()
        )
        outputRecord(type = RecordType.RecordParam, record = paramRecord)
    }

    fun outputParamReturnRecord(
        paramValues: Array<out Any?>,
        returnValue: Any?,
        hookConfig: HookConfig
    ) {
        val paramReturnRecord = RecordParamReturn(
            className = hookConfig.className,
            methodName = hookConfig.methodName,
            params = hookConfig.params.split(","),
            paramValues = paramValues.map { it.recordValue },
            returnValue = returnValue.recordValue,
            callStack = getStackTrace()
        )
        outputRecord(
            type = RecordType.RecordParamReturn,
            record = paramReturnRecord
        )
    }


    fun outputReturnRecord(returnValue: Any?, hookConfig: HookConfig) {
        val returnRecord = RecordReturn(
            className = hookConfig.className,
            methodName = hookConfig.methodName,
            params = hookConfig.params.split(","),
            returnValue = returnValue.recordValue,
            callStack = getStackTrace()
        )
        outputRecord(type = RecordType.RecordReturn, record = returnRecord)
    }


    fun outputRecord(type: RecordType, record: Record) {
        HookHelper.appContext.getExternalFilesDirs("")
        val recordContent = Json.encodeToString(
            RecordEntity(
                type = type,
                record = Json.encodeToString(record),
                packageName = HookHelper.hostPackageName,
                time = TimeUtil.getCurrentTime(pattern = "yy-MM-dd HH:mm:ss")
            )
        )

        RecordLogger.write(packageName = HookHelper.hostPackageName, content = recordContent)

        /* FileUtils.outTextToFile(
             filePath = recordPath,
             content = recordContent,
             isNewLine = true,
             limitSize = 4096,
             append = true
         )*/
    }

    fun outputBase64(operation: Base64Operation, rawData: ByteArray, resultData: ByteArray) {
        val base64Record = RecordBase64(
            operation = operation,
            rawData = rawData.recordValue,
            result = resultData.recordValue,
            stackDetail = Throwable().stackTraceToString()
        )
        outputRecord(type = RecordType.Base64, record = base64Record)
    }

    fun outputClipboard(isRead: Boolean, info: String) {
        outputRecord(
            type = RecordType.Clipboard,
            record = RecordClipboard(
                isRead = isRead,
                info = info,
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputExitRecord(type: String) {
        outputRecord(
            type = RecordType.Exit, record = RecordExit(
                exitType = type,
                stackDetail = Throwable().stackTraceToString()
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
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputSignature(signByteArray: ByteArray) {
        outputRecord(
            type = RecordType.Signature, record = RecordSignature(
                md5 = ToolUtil.getDigest(bytes = signByteArray),
                sha1 = ToolUtil.getDigest(bytes = signByteArray, "SHA-1"),
                sha256 = ToolUtil.getDigest(bytes = signByteArray, "SHA-256"),
                charStr = String(signByteArray),
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputJson(type: RecordJsonType, values: Map<String, String>) {
        val jsonRecord = RecordJson(
            jsonType = type,
            values = values,
            stackDetail = Throwable().stackTraceToString()
        )
        outputRecord(type = RecordType.Json, record = jsonRecord)
    }

    fun outputDialog(type: RecordDialogType, textList: List<String>) {
        outputRecord(
            type = RecordType.Dialog, record = RecordDialog(
                dialogType = type,
                textList = textList,
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputPopup(type: RecordPopupWindowType, textList: List<String>) {
        outputRecord(
            type = RecordType.PopupWindow, record = RecordPopupWindow(
                popupType = type,
                textList = textList,
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputToast(textList: List<String>) {
        outputRecord(
            type = RecordType.Toast, record = RecordToast(
                textList = textList,
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputHmac(algorithm: String, rawData: ByteArray, resultData: ByteArray) {
        outputRecord(
            type = RecordType.Hmac, record = RecordHmac(
                algorithm = algorithm,
                rawData = rawData.recordValue,
                resultData = resultData.recordValue,
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

    fun outputMac(
        algorithm: String,
        key: Map<RecordValueType, String>?,
        rawData: ByteArray,
        resultData: ByteArray
    ) {
        outputRecord(
            type = RecordType.Mac, record = RecordMac(
                algorithm = algorithm,
                key = key,
                rawData = rawData.recordValue,
                resultData = resultData.recordValue,
                stackDetail = Throwable().stackTraceToString()
            )
        )
    }

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


    private fun getStackTrace(): List<String> {
        return Throwable().stackTrace.map { element ->
            "${element.className} --> ${element.methodName}(line：${element.lineNumber})"
        }
    }

    private val Any?.recordValue: Map<RecordValueType, String>
        get() {
            return buildMap {
                put(RecordValueType.ToString, this@recordValue.toString())
                put(RecordValueType.GsonToString, Gson().toJson(this@recordValue))
            }
        }

    val ByteArray.recordValue: Map<RecordValueType, String>
        get() {
            return buildMap {
                put(RecordValueType.BytesToString, String(this@recordValue))
                put(
                    RecordValueType.Base64, GuiseBase64.encodeToString(
                        this@recordValue,
                        GuiseBase64.DEFAULT
                    )
                )
                put(RecordValueType.Hex, this@recordValue.toHex())
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
            // 将字节转为无符号整数（0-255），避免负数影响
            val unsignedByte = b.toInt() and 0xFF
            // 高4位 = 无符号字节 ÷ 16（等价于右移4位）
            val highNibble = unsignedByte / 16
            // 低4位 = 无符号字节 % 16（等价于 & 0x0F）
            val lowNibble = unsignedByte % 16

            hexBuilder.append(hexChars[highNibble])
            hexBuilder.append(hexChars[lowNibble])
        }
        return hexBuilder.toString()
    }


    fun String.toHex(charset: Charset = Charsets.UTF_8, lowercase: Boolean = false): String {
        return this.toByteArray(charset).toHex(lowercase)
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