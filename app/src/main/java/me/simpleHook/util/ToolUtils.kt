package me.simpleHook.util

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

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
    fun toClip(context: Context, configs: String) {
        (context.getSystemService("clipboard") as ClipboardManager).setPrimaryClip(
            ClipData.newPlainText(
                "label", configs
            )
        )
    }

    fun getDigest(bytes: ByteArray, algorithm: String = "MD5"): String {
        return try {
            val digest: ByteArray = MessageDigest.getInstance(algorithm).digest(bytes)
            val hexDigits = "0123456789abcdef"
            val str = CharArray(digest.size * 2)
            var k = 0
            for (b in digest) {
                str[k++] = hexDigits[b.toInt() ushr 4 and 0xf]
                str[k++] = hexDigits[b.toInt() and 0xf]
            }
            String(str)
        } catch (e: NoSuchAlgorithmException) {
            ""
        }
    }
}