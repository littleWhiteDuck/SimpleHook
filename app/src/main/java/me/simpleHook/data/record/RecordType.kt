package me.simpleHook.data.record

import me.simpleHook.R

enum class RecordType(val displayId: Int) {
    Error(displayId = 1),
    RecordParam(displayId = 1),
    RecordReturn(displayId = 1),
    RecordParamReturn(displayId = 1),
    RecordFiled(displayId = 1),
    Base64(displayId = 1),
    Application(displayId = 1),
    CrashCaught(displayId = 1),
    ClickEvent(displayId = 1),
    Clipboard(displayId = 1),
    Exit(displayId = 1),
    FileOperation(displayId = 1),
    Signature(displayId = 1),
    Json(displayId = 1),
    Dialog(displayId = 1),
    PopupWindow(displayId = 1),
    Toast(displayId = 1),
    WebLoadUrl(displayId = 1),
    Hmac(displayId = 1),
    Mac(displayId = 1),
    Cipher(displayId = 1),
    Intent(displayId = 1),
}

enum class RecordFileOpType(val displayId: Int) {
    Create(displayId = R.string.record_file_read),
    Delete(displayId = R.string.record_file_delete),
    Read(displayId = R.string.record_file_read),
    Write(displayId = R.string.record_file_write),
    Assets(displayId = R.string.record_file_assets),
}


enum class RecordValueType(val displayName: String) {
    ToString("toString"),
    GsonToString("Gson"),
    BytesToString(displayName = "string"),
    Base64("base64"),
    Hex("hex")
}

enum class RecordErrorType(val displayId: Int) {
    Class(displayId = R.string.record_error_class),
    Method(displayId = R.string.record_error_method),
    Field(displayId = R.string.record_error_field),
    Other(displayId = R.string.record_error_other)
}
