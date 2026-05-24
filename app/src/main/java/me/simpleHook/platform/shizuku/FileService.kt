package me.simpleHook.platform.shizuku

import me.simpleHook.core.constant.ConfigConstant
import me.simpleHook.core.utils.FileUtil
import me.simpleHook.core.utils.SuUtil
import java.io.File

class FileService : IFileService.Stub() {

    override fun listFiles(dirPath: String): List<String> {
        return runCatching {
            if (ShizukuFileManager.rootMode) {
                File(dirPath).listFiles()?.map { it.name } ?: emptyList()
            } else {
                SuUtil.listFileNames(dirPath)
            }
        }.onFailure { it.printStackTrace() }.getOrDefault(emptyList())
    }

    override fun copyFile(scrPath: String, desPath: String): Boolean {
        // Android/media directory can use file API directly, no shell command required.
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

    override fun deleteFiles(paths: List<String>): Boolean {
        return paths.all(::deleteFile)
    }

    override fun forceStopPackage(packageName: String) {
        SuUtil.forceStopApp(packageName)
    }

    override fun reLaunchApp(packageName: String, activityName: String) {
        SuUtil.reLaunchApp(packageName = packageName, activityName = activityName)
    }


}
