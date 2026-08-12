package me.simpleHook.platform.shizuku

import me.simpleHook.core.utils.FileUtil
import java.io.File

class FileService : IFileService.Stub() {

    override fun listFiles(dirPath: String): List<String> {
        return runCatching {
            File(dirPath).listFiles()?.map { it.name }.orEmpty()
        }.onFailure { it.printStackTrace() }.getOrDefault(emptyList())
    }

    override fun copyFile(scrPath: String, desPath: String): Boolean {
        // Android/media directory can use file API directly, no shell command required.
        return FileUtil.copyFile(scrPath, desPath)
    }

    override fun writeFile(path: String, content: String): Boolean {
        return FileUtil.outTextToFile(filePath = path, content = content)
    }


    override fun deleteFile(path: String): Boolean = runCatching {
        FileUtil.deleteFile(filePath = path)
    }.onFailure { it.printStackTrace() }.getOrDefault(false)

    override fun deleteFiles(paths: List<String>): Boolean {
        return paths.all(::deleteFile)
    }

    override fun forceStopPackage(packageName: String) {
        runCommand("am", "force-stop", packageName)
    }

    override fun reLaunchApp(packageName: String, activityName: String) {
        runCommand("am", "force-stop", packageName)
        runCommand("am", "start", "$packageName/$activityName")
    }

    private fun runCommand(vararg command: String): Boolean {
        return runCatching {
            ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        }.onFailure { it.printStackTrace() }.getOrDefault(false)
    }

}
