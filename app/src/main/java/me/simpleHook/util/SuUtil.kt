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
                Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER).setTimeout(20)
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

    fun forceStopApp(packageName: String) {
        Shell.cmd("am force-stop $packageName").exec()
    }

    fun reLaunchApp(packageName: String, activityName: String) {
        Shell.cmd(
            "am force-stop $packageName", "am start ${packageName}/$activityName"
        ).exec()
    }

    fun deleteFile(filePath: String): Boolean {
        return Shell.cmd("rm -rf $filePath").exec().isSuccess
    }

    fun makeDirs(filePath: String): Boolean {
        return Shell.cmd("mkdir -p $filePath").exec().isSuccess
    }

}