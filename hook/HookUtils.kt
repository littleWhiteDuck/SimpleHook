package me.simpleHook.hook

import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.children

fun getAllTextView(viewGroup: ViewGroup): List<String> {
    val list = mutableListOf<String>()
    viewGroup.children.forEach {
        when (it) {
            is Button -> {
                if (it.text.toString().isNotEmpty()) {
                    list.add(Tip.getTip("button") + it.text.toString())
                }
            }
            is TextView -> {
                if (it.text.toString().isNotEmpty()) {
                    list.add(Tip.getTip("text") + it.text.toString())
                }
            }
            is ViewGroup -> {
                list += getAllTextView(it)
            }
        }
    }
    return list
}

fun byte2Sting(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (b in bytes) {
        if (Integer.toHexString(0xFF and b.toInt()).length == 1) {
            sb.append("0")
        }
        sb.append(Integer.toHexString(0xFF and b.toInt()))
    }
    return sb.toString()
}