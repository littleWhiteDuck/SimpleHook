package me.simpleHook.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object AssetsUtil {
    fun getText(context: Context, fileName: String): String? {
        return getText(context.assets.open(fileName))
    }

    fun getText(inputStream: InputStream): String? {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        return try {
            bufferedReader.readText()
        } catch (e: Exception) {
            null
        } finally {
            bufferedReader.close()
        }
    }
}