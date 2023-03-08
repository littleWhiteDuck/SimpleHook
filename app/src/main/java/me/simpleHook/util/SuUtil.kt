package me.simpleHook.util


import com.topjohnwu.superuser.Shell

object SuUtil {
    var isInit = false
    fun init() {
        if (!isInit) {
            Thread {
                isInit = true
                Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(20))
            }.start()
        }
    }

    fun forceStopApp(packageName: String) {
        Shell.cmd("am force-stop $packageName").exec()
    }

    fun reLaunchApp(packageName: String, activityName: String) {
        Shell.cmd("am force-stop $packageName", "am start ${packageName}/$activityName").exec()
    }

    fun deleteFile(filePath: String): Boolean {
        return Shell.cmd("rm -rf $filePath").exec().isSuccess
    }

    fun makeDirs(filePath: String): Boolean {
        return Shell.cmd("mkdir -p $filePath").exec().isSuccess
    }

    fun chmodConfigFile(path: String): Boolean {
        return Shell.cmd("chmod -R 644 $path").exec().isSuccess
    }

    fun isGrantedRoot(): Boolean {
        return (Shell.isAppGrantedRoot() == true).also {
            if (!it) {
                su()
            }
        }
    }

    private fun su(): Boolean {
        return Shell.cmd("su").exec().isSuccess
    }

}