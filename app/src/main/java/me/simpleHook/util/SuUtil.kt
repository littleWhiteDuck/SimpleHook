package me.simpleHook.util


import android.content.Context
import android.os.Looper
import com.topjohnwu.superuser.Shell
import kotlin.concurrent.thread

object SuUtil {
    var isInit = false
    fun init(context: Context) {
        if (!isInit) {
            isInit = true
            Shell.setDefaultBuilder(
                Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR).setTimeout(20)
            )
        }
        thread {
            Shell.cmd(
                "su",
                "mount -o remount /data",
                "cd /data/local/tmp",
                "mkdir -p simpleHook/logTemp/",
                "chmod -R 777 simpleHook"
            ).exec()
            if (Shell.isAppGrantedRoot() != true) {
                Looper.prepare()
                "拒绝或没有Root权限".toast(context)
                Looper.loop()
            }
        }
    }

    fun moveFile(originalPath: String, finalPath: String) {
        Shell.cmd("mv $originalPath $finalPath").exec()
    }

}