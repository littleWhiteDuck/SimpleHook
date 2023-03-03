package me.simpleHook.worker

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.work.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.database.AppRepository
import me.simpleHook.extension.showToast
import me.simpleHook.util.FileUtils
import me.simpleHook.util.TimeUtil
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupHelper {
    private val json = Json {
        prettyPrint = true
    }

    fun localBackupConfig(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<BackupWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        WorkManager.getInstance(context).enqueue(request)
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id).observeForever {
            if (it.state == WorkInfo.State.FAILED) {
                context.showToast(context.getString(R.string.backup_tip_local_auto_backup_failed))
            }
        }
    }

    fun cloudBackupConfig(context: Context) {
        val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
        val request = OneTimeWorkRequestBuilder<BackupWorker>().setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
        WorkManager.getInstance(context).enqueue(request)
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id).observeForever {
            if (it.state == WorkInfo.State.FAILED) {
                context.showToast(context.getString(R.string.backup_tip_cloud_auto_backup_failed))
            }
        }
    }

    @Synchronized
    fun startBackupConfig(
        context: Context,
        backupCustom: Boolean,
        backupExtension: Boolean,
        backupAll: Boolean,
        local: Boolean = false,
        cloud: Boolean = false,
        backUri: Uri? = null
    ): Boolean {
        return runCatching {
            val cacheBackupDir = context.externalCacheDir!!.resolve("backup").also {
                it.mkdir()
            }
            val appRepository = AppRepository(context)
            val customConfigs =
                if (backupAll || backupCustom) appRepository.getConfigs() else emptyList()
            val extensionConfigs =
                if (backupAll || backupExtension) appRepository.getAssistConfigs() else emptyList()
            if (customConfigs.isEmpty() && extensionConfigs.isEmpty()) return@runCatching true
            val backupName =
                "SimpleHook-Custom(${customConfigs.size})-Ex(${extensionConfigs.size})-${getTime()}-${Build.MODEL}.shbackup"
            val cacheFile = cacheBackupDir.resolve(backupName)
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(cacheFile.outputStream()))
            zipOutputStream.use {
                if (backupAll || backupCustom) {
                    val zipEntry = ZipEntry("custom_config.json")
                    zipOutputStream.putNextEntry(zipEntry)
                    zipOutputStream.write(json.encodeToString(customConfigs).toByteArray())
                }
                if (backupAll || backupExtension) {
                    val zipEntry = ZipEntry("extension_config.json")
                    zipOutputStream.putNextEntry(zipEntry)
                    zipOutputStream.write(json.encodeToString(extensionConfigs).toByteArray())
                }
            }
            var result = true
            if (local) {
                val tempUri = backUri ?: DocumentCompat.getFileUriOrCreate(context,
                    GlobalValue.sp.backup_path!!.toUri(),
                    "SimpleHook/Backups",
                    backupName,
                    "application/shbackup")!!
                context.contentResolver.openOutputStream(tempUri).use { output ->
                    cacheFile.inputStream().use {
                        if (output != null) {
                            it.copyTo(output)
                        }
                    }
                }
            }
            if (cloud) {
                result = cloudBackup(cacheFile)
            }
            result
        }.onFailure { FileUtils.deleteDir(context.externalCacheDir!!.resolve("cache")) }
            .onSuccess { FileUtils.deleteDir(context.externalCacheDir!!.resolve("cache")) }
            .getOrDefault(false)
    }

    private fun getTime(): String {
        val pattern = when (GlobalValue.sp.backup_cover) {
            "BACKUP_OVER_MINUTE" -> "yyyy-MM-dd-HH-mm"
            "BACKUP_OVER_HOUR" -> "yyyy-MM-dd-HH"
            else -> "yyyy-MM-dd"
        }
        return TimeUtil.getCurrentTime(pattern)
    }

    private fun cloudBackup(file: File) = runCatching {
        CloudBackupHelper.uploadFile(file)
    }.getOrDefault(false)

}