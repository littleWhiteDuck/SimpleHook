package me.simpleHook.util

import me.simpleHook.SimpleHookApp

object LogUtils {
    fun outLog(error: String) {
        FileUtils.outTextToFile(
            content = error,
            filePath = SimpleHookApp.app.getExternalFilesDir(null)!!.path + "/log.txt",
            limitSize = 512,
            isNewLine = true,
            append = true
        )
    }
}