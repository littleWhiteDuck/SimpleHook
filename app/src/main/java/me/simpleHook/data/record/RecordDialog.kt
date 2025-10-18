package me.simpleHook.data.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.simpleHook.R


enum class RecordDialogType(val displayId: Int) {
    Record(displayId = R.string.record_dialog_listen),
    BlockKeyword(displayId = R.string.record_dialog_block_keyword),
    BlockId(displayId = R.string.record_dialog_block_id)
}

@Serializable
@SerialName("Dialog")
data class RecordDialog(
    val dialogType: RecordDialogType,
    val textList: List<String>,
    val stackDetail: String,
) : Record()
