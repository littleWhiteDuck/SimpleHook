package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ToolUtils {
    fun getClipboardContent(context: Context): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val data = cm.primaryClip
        if (data != null && data.itemCount > 0) {
            val item = data.getItemAt(0)
            if (item != null) {
                val sequence = item.coerceToText(context)
                if (sequence != null) {
                    return sequence.toString()
                }
            }
        }
        return null
    }
    @SuppressLint("WrongConstant")
    fun toClip(context: Context, configs:String){
        (context.getSystemService("clipboard") as ClipboardManager).setPrimaryClip(
            ClipData.newPlainText(
                "label",
                configs
            )
        )
    }
}