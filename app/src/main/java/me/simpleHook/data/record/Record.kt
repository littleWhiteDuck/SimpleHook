package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.simpleHook.data.HookConfig


@Serializable
sealed class Record()


@Serializable
@SerialName("RecordError")
data class RecordError(
    val errorType: RecordErrorType,
    val hookConfig: HookConfig? = null,
    val stackDetail: String,
    val supplement: String? = null
) : Record()


@Serializable
@SerialName("RecordParam")
data class RecordParam(
    val className: String,
    val methodName: String,
    val params: List<String> = emptyList(),
    val paramValues: List<Map<RecordValueType, String>>,
    val callStack: List<String> = emptyList()
) : Record()

@Serializable
@SerialName("RecordParamReturn")
data class RecordParamReturn(
    val className: String,
    val methodName: String,
    val params: List<String> = emptyList(),
    val paramValues: List<Map<RecordValueType, String>>,
    val returnValue: Map<RecordValueType, String>,
    val callStack: List<String> = emptyList()
) : Record()

@Serializable
@SerialName("RecordReturn")
data class RecordReturn(
    val className: String,
    val methodName: String,
    val params: List<String> = emptyList(),
    val returnValue: Map<RecordValueType, String>,
    val callStack: List<String> = emptyList()
) : Record()


@Serializable
@SerialName("RecordField")
data class RecordField(
    val className: String? = null,
    val methodName: String? = null,
    val params: List<String> = emptyList(),
    val fieldClassName: String? = null,
    val fieldName: String,
    val filedValue: Map<RecordValueType, String>,
    val callStack: List<String> = emptyList()
) : Record()




@Serializable
@SerialName("Application")
data class RecordApplication(
    val name: String
) : Record()

@Serializable
@SerialName("CrashCaught")
data class RecordCrash(
    val threadName: String,
    val stackDetail: String,
) : Record()


@Serializable
@SerialName("ClickEvent")
data class RecordClickEvent(
    val viewType: String,
    val callbackType: String,
    val viewId: String?,
    val textList: List<String>,
    val stackDetail: String,
) : Record()


@Serializable
@SerialName("Clipboard")
data class RecordClipboard(
    val isRead: Boolean,
    val info: String,
    val stackDetail: String,
) : Record()

@Serializable
@SerialName("Exit")
data class RecordExit(
    val exitType: String,
    val stackDetail: String,
) : Record()

@Serializable
@SerialName("FileOperation")
data class RecordFileOperation(
    val operation: RecordFileOpType,
    val path: String,
    val partData: String? = null,
    val stackDetail: String,
) : Record()

@Serializable
@SerialName("Signature")
data class RecordSignature(
    val md5: String? = null,
    val sha1: String? = null,
    val sha256: String? = null,
    val charStr: String? = null,
    val stackDetail: String,
) : Record()




