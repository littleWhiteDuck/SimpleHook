package me.simpleHook.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import me.simpleHook.GlobalValue
import me.simpleHook.worker.BackupWorkerHelper.startBackupConfig

class BackupWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val scope = GlobalValue.sp.backup_scope
        val result = startBackupConfig(context,
            scope == "BACKUP_SCOPE_CUSTOM",
            scope == "BACKUP_SCOPE_EXTENSION",
            scope == "BACKUP_SCOPE_ALL",
            local = GlobalValue.sp.backup_local_auto,
            cloud = GlobalValue.sp.backup_cloud_auto)
        return if (result) Result.success() else Result.failure()
    }


}