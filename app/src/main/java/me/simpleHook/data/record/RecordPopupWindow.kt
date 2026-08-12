package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.simpleHook.R

enum class RecordPopupWindowType(val displayId: Int) {
    Record(displayId = R.string.record_popup_listen),
    BlockKeyword(displayId = R.string.record_popup_block_keyword),
    BlockId(displayId = R.string.record_popup_block_id)
}

@Serializable
@SerialName("PopupWindow")
data class RecordPopupWindow(
    val popupType: RecordPopupWindowType,
    val textList: List<String>,
    val stackDetail: String,
) : Record()

