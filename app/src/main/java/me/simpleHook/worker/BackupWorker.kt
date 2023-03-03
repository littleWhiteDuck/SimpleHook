package me.simpleHook.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.util.OSUtils
import me.simpleHook.worker.BackupHelper.startBackupConfig

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

    override fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (OSUtils.atLeastO()) {
            val channel =
                NotificationChannel("Backup", "Backup", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "Backup")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(R.string.backup_tip_running_backup))
            .build()
        return ForegroundInfo(1337, notification)
    }


}