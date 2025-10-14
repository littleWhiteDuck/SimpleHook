package me.simpleHook.shizuku

import me.simpleHook.constant.ConfigConstant
import me.simpleHook.utils.FileUtil
import me.simpleHook.utils.SuUtil

class FileService : IFileService.Stub() {

    override fun copyFile(scrPath: String, desPath: String): Boolean {
        // android/data directory can use the file api , there is no need to use `shell`
        return FileUtil.copyFile(scrPath, desPath)
    }

    override fun writeFile(path: String, content: String): Boolean {
        if (ShizukuFileManager.rootMode) {
            return FileUtil.outTextToFile(filePath = path, content = content)
        } else {
            FileUtil.outTextToFile(filePath = ConfigConstant.TEMP_CONFIG_PATH, content)
            return SuUtil.mvAndChmod(srcPath = ConfigConstant.TEMP_CONFIG_PATH, destPath = path)
        }

    }


    override fun deleteFile(path: String): Boolean = runCatching {
        if (ShizukuFileManager.rootMode) {
            FileUtil.deleteFile(filePath = path)
        } else {
            SuUtil.deleteFile(filePath = path)
        }
    }.onFailure { it.printStackTrace() }.getOrDefault(false)

    override fun forceStopPackage(packageName: String) {
        SuUtil.forceStopApp(packageName)
    }

    override fun reLaunchApp(packageName: String, activityName: String) {
        SuUtil.reLaunchApp(packageName = packageName, activityName = activityName)
    }


}