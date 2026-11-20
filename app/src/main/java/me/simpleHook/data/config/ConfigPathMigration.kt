package me.simpleHook.data.config

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import me.simpleHook.core.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.local.db.AppDatabase

object ConfigPathMigration {
    private var migrationJob: Job? = null

    fun migrateIfNeeded(context: Context, scope: CoroutineScope) {
        if (GlobalValue.sp.mediaPathConfigMigrated || migrationJob?.isActive == true) return
        migrationJob = scope.launch(Dispatchers.IO) {
            try {
                ConfigWriteCoordinator.write { runMigration(context.applicationContext) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.outLog(error.stackTraceToString())
            }
        }
    }

    private suspend fun runMigration(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val configSystem = ConfigSystemUtil.getConfigSystem()
        var allSuccess = true

        val migratedCustomPackages = HashSet<String>()
        db.getAppConfigDao().getAll().forEach { appConfig ->
            if (!appConfig.enable) return@forEach
            if (!migratedCustomPackages.add(appConfig.packageName)) return@forEach
            val content = Json.encodeToString(appConfig)
            if (!ConfigRemoteSyncHelper.saveCustomConfig(configSystem, appConfig.packageName, content).isComplete) {
                allSuccess = false
            }
        }

        val migratedExtensionPackages = HashSet<String>()
        db.getExtensionConfigDao().getExtConfigs().forEach { extConfig ->
            if (extConfig.packageName == Constant.MODEL_EXTENSION_CONFIG) return@forEach
            if (!extConfig.enable) return@forEach
            if (!migratedExtensionPackages.add(extConfig.packageName)) return@forEach
            if (!ConfigRemoteSyncHelper.saveExtensionConfig(
                    configSystem,
                    extConfig.packageName,
                    extConfig.config
                ).isComplete
            ) {
                allSuccess = false
            }
        }

        if (allSuccess) {
            GlobalValue.sp.mediaPathConfigMigrated = true
        }
    }
}
