package me.simpleHook.utils

import me.simpleHook.App

object LogUtils {
    @JvmStatic
    fun outLog(error: String) {
        if (FlavorUtil.betaVersion) {
            FileUtil.outTextToFile(content = error,
                filePath = App.getExternalFilesDir(null)!!.path + "/log.txt",
                limitSize = 512,
                isNewLine = true,
                append = true)
        }

    }
}