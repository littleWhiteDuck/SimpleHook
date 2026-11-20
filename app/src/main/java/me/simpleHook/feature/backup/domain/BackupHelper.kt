package me.simpleHook.feature.backup.domain

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.simpleHook.data.local.db.AppDatabase
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.serialization.json.Json
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.compat.DocumentCompat
import me.simpleHook.data.local.db.AppRepository
import me.simpleHook.core.utils.TimeUtil
import java.io.BufferedOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupHelper {
    private val backupMutex = Mutex()
    private val json = Json {
        prettyPrint = true
    }

    fun localBackupConfig(context: Context, ID: UUID) {
        val request =
            OneTimeWorkRequestBuilder<BackupWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setId(ID).setInputData(workDataOf("LOCAL" to true)).build()
        WorkManager.getInstance(context).enqueue(request)

    }

    fun cloudBackupConfig(context: Context, ID: UUID) {
        val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
        val request =
            OneTimeWorkRequestBuilder<BackupWorker>().setConstraints(constraints).setId(ID)
                .setInputData(workDataOf("CLOUD" to true))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
        WorkManager.getInstance(context).enqueue(request)
    }

    suspend fun startBackupConfig(
        context: Context,
        backupCustom: Boolean,
        backupExtension: Boolean,
        backupCollection: Boolean,
        backupAll: Boolean,
        local: Boolean = false,
        cloud: Boolean = false,
        backUri: Uri? = null
    ): Boolean = backupMutex.withLock {
        withContext(Dispatchers.IO) {
            val cacheBackupDir = File(context.cacheDir, "backup-${UUID.randomUUID()}")
            try {
                check(cacheBackupDir.mkdirs())
                val appRepository = AppRepository(context)
                val snapshot = AppDatabase.getDatabase(context).withTransaction {
                    Triple(
                        if (backupAll || backupCustom) appRepository.getConfigs() else emptyList(),
                        if (backupAll || backupExtension) appRepository.getExtConfigs() else emptyList(),
                        if (backupAll || backupCollection) appRepository.getCollections() else emptyList()
                    )
                }
                val (customConfigs, extensionConfigs, collections) = snapshot
                currentCoroutineContext().ensureActive()
                val backupName =
                    "SimpleHook-Custom(${customConfigs.size})-Ex(${extensionConfigs.size})-Co(${collections.size})-${getTime()}-${Build.MODEL}.shbackup"
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
                    if (backupAll || backupCollection) {
                        val zipEntry = ZipEntry("collection_config.json")
                        zipOutputStream.putNextEntry(zipEntry)
                        zipOutputStream.write(json.encodeToString(collections).toByteArray())
                    }
                }
                var result = true
                if (local) {
                    result = runCatching {
                        val tempUri = backUri ?: DocumentCompat.getFileUriOrCreate(
                            context,
                            GlobalValue.sp.backup_path!!.toUri(),
                            "SimpleHook/Backups",
                            backupName,
                            "application/shbackup"
                        )!!
                        checkNotNull(context.contentResolver.openOutputStream(tempUri)).use { output ->
                            cacheFile.inputStream().use {
                                it.copyTo(output)
                            }
                        }
                        true
                    }.getOrDefault(false)
                }
                if (cloud) {
                    currentCoroutineContext().ensureActive()
                    result = cloudBackup(cacheFile) && result
                }
                result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d("littleWhiteDuck", "backup: ${error.stackTraceToString()}")
                false
            } finally {
                cacheBackupDir.deleteRecursively()
            }
        }
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

