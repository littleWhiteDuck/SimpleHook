package me.simpleHook.core.utils

import android.content.Context
import java.io.File

object ApkExtractionManager {

    fun extractPackage(context: Context, packageName: String, destinationDir: File): ExtractionResult {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        return if (isSplitApk(context, packageName)) {
            val files = extractSplitApks(context, packageName, destinationDir)
            if (files.isEmpty()) {
                ExtractionResult.Failure("Failed to extract APK")
            } else {
                ExtractionResult.Success(files, true)
            }
        } else {
            val file = extractSingleApk(context, packageName, destinationDir)
            if (file != null) {
                ExtractionResult.Success(listOf(file), false)
            } else {
                ExtractionResult.Failure("Failed to extract APK")
            }
        }
    }

    private fun extractSingleApk(context: Context, packageName: String, destinationDir: File): File? =
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val sourceFile = File(packageInfo.applicationInfo!!.sourceDir)
            val fileName = "${getSafeAppName(context, packageName)}_${packageName}.apk"
            val destFile = File(destinationDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            destFile
        }.getOrNull()

    private fun extractSplitApks(
        context: Context,
        packageName: String,
        destinationDir: File
    ): List<File> {
        val files = mutableListOf<File>()
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageInfo.applicationInfo ?: return emptyList()

            File(appInfo.sourceDir).copyTo(File(destinationDir, "base.apk"), overwrite = true)
                .also(files::add)

            appInfo.splitSourceDirs.orEmpty().forEach { path ->
                File(path).copyTo(File(destinationDir, File(path).name), overwrite = true)
                    .also(files::add)
            }
        } catch (_: Exception) {
        }
        return files
    }

    private fun isSplitApk(context: Context, packageName: String): Boolean = runCatching {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        !packageInfo.applicationInfo?.splitSourceDirs.isNullOrEmpty()
    }.getOrDefault(false)

    private fun getSafeAppName(context: Context, packageName: String): String = runCatching {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(appInfo)
            .toString()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }.getOrDefault(packageName)

    sealed class ExtractionResult {
        data class Success(val files: List<File>, val isSplit: Boolean) : ExtractionResult()
        data class Failure(val message: String) : ExtractionResult()
    }
}
