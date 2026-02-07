package me.simpleHook.utils

import me.simpleHook.data.record.RecordType

object RecordTypeUtils {

    fun getShowText(type: RecordType) = when (type) {
        RecordType.Error -> type.name
        RecordType.RecordParam -> "Param"
        RecordType.RecordReturn -> "Return"
        RecordType.RecordParamReturn -> "P&R"
        RecordType.RecordField -> "Field"
        RecordType.Base64 -> type.name
        RecordType.Application -> "App"
        RecordType.CrashCaught -> type.name
        RecordType.ClickEvent -> "Click"
        RecordType.Clipboard -> "Clip"
        RecordType.Exit -> type.name
        RecordType.FileOperation -> "File"
        RecordType.Signature -> "Sign"
        RecordType.Json -> "JSON"
        RecordType.Dialog -> "Dialog"
        RecordType.PopupWindow -> "Popup"
        RecordType.Toast -> "Toast"
        RecordType.WebLoadUrl -> "Web"
        RecordType.Hmac -> "HMAC"
        RecordType.Mac -> "MAC"
        RecordType.Cipher -> "Cipher"
        RecordType.Intent -> "Intent"
    }

}