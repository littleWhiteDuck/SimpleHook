package me.simpleHook.util

import me.simpleHook.App

object LogUtils {
    @JvmStatic
    fun outLog(error: String) {
        FileUtils.outTextToFile(content = error,
            filePath = App.getExternalFilesDir(null)!!.path + "/log.txt",
            limitSize = 512,
            isNewLine = true,
            append = true)
    }
}