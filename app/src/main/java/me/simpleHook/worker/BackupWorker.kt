package me.simpleHook.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import me.simpleHook.GlobalValue
import me.simpleHook.worker.BackupWorkerHelper.outConfigs

class BackupWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        if (!GlobalValue.sp.backup_auto) return Result.success()
        val scope = GlobalValue.sp.backup_scope
        val result = outConfigs(context,
            scope == "BACKUP_SCOPE_CUSTOM",
            scope == "BACKUP_SCOPE_EXTENSION",
            scope == "BACKUP_SCOPE_ALL")
        return if (result) Result.success() else Result.failure()
    }


}