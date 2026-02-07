package me.simpleHook.data.record

import me.simpleHook.R

enum class RecordType(val displayId: Int) {
    Error(displayId = R.string.record_type_error),
    RecordParam(displayId = R.string.record_type_param),
    RecordReturn(displayId = R.string.record_type_return),
    RecordParamReturn(displayId = R.string.record_type_param_and_return),
    RecordField(displayId = R.string.record_type_field),
    Base64(displayId = R.string.record_type_base64),
    Application(displayId = R.string.record_type_application),
    CrashCaught(displayId = R.string.record_type_crash_caught),
    ClickEvent(displayId = R.string.record_type_click_event),
    Clipboard(displayId = R.string.record_type_clipboard),
    Exit(displayId = R.string.record_type_exit),
    FileOperation(displayId = R.string.record_type_file_operation),
    Signature(displayId = R.string.record_type_signature),
    Json(displayId = R.string.record_type_json),
    Dialog(displayId = R.string.record_type_dialog),
    PopupWindow(displayId = R.string.record_type_popup_window),
    Toast(displayId = R.string.record_type_toast),
    WebLoadUrl(displayId = R.string.record_type_web_url),
    Hmac(displayId = R.string.record_type_hmac),
    Mac(displayId = R.string.record_type_mac),
    Cipher(displayId = R.string.record_type_cipher),
    Intent(displayId = R.string.record_type_Intent),
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
    BytesToString(displayName = "String"),
    Base64("Base64"),
    Hex("Hex")
}

enum class RecordErrorType(val displayId: Int) {
    Class(displayId = R.string.record_error_class),
    Method(displayId = R.string.record_error_method),
    Field(displayId = R.string.record_error_field),
    Other(displayId = R.string.record_error_other)
}
