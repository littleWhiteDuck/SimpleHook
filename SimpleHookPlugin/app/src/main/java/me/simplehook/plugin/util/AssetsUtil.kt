package me.simplehook.plugin.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object AssetsUtil {
    fun getText(context: Context, fileName: String): String? {
        return getText(context.assets.open(fileName))
    }

    fun getText(inputStream: InputStream): String? {
        return runCatching {
            BufferedReader(InputStreamReader(inputStream)).use {
                it.readText()
            }
        }.getOrNull()
    }
}