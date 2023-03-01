package me.simpleHook.worker

import android.content.Context
import androidx.core.net.toUri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalValue
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.database.AppRepository
import me.simpleHook.util.TimeUtil
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupWorkerHelper {
    private val json = Json {
        prettyPrint = true
    }

    fun nowBackupConfig(context: Context) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun outConfigs(
        context: Context, backupCustom: Boolean, backupExtension: Boolean, backupAll: Boolean
    ): Boolean {
        return runCatching {
            val appRepository = AppRepository(context)
            val customConfigs = appRepository.getConfigs()
            val extensionConfigs = appRepository.getAssistConfigs()
            if (customConfigs.isEmpty() && extensionConfigs.isEmpty()) return@runCatching true
            val backupName =
                "SimpleHook-Custom(${customConfigs.size})-Ex(${extensionConfigs.size})-${getTime()}.shbackup"
            val documentFile = DocumentCompat.getFileUriOrCreate(context,
                GlobalValue.sp.backup_path!!.toUri(),
                "SimpleHook/Backups",
                backupName,
                "application/shbackup")
            val zipOutputStream =
                ZipOutputStream(BufferedOutputStream(context.contentResolver.openOutputStream(
                    documentFile!!)))
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
            true
        }.getOrDefault(false)
    }

    private fun getTime(): String {
        val pattern = when (GlobalValue.sp.backup_cover) {
            "BACKUP_OVER_MINUTE" -> "yyyy-MM-dd-HH-mm"
            "BACKUP_OVER_HOUR" -> "yyyy-MM-dd-HH"
            else -> "yyyy-MM-dd"
        }
        return TimeUtil.getCurrentTime(pattern)
    }

}