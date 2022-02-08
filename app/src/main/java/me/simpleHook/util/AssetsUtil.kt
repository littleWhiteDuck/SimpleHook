package me.simpleHook.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object AssetsUtil {
    fun getText(context: Context, fileName: String): String {
        val bufferedReader = BufferedReader(InputStreamReader(context.assets.open(fileName)))
        return try {
            var msg = ""
            bufferedReader.readLines().forEach {
                msg += it + "\n"
            }
            msg.substring(0, msg.length - 1)
        } catch (e: Exception) {
            "失败！"
        } finally {
            bufferedReader.close()
        }
    }
}