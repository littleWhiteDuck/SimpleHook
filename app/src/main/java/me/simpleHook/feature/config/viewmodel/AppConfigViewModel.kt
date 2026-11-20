package me.simpleHook.feature.config.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.simpleHook.R
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.extension.showToast
import me.simpleHook.core.utils.LogUtil
import me.simpleHook.data.config.ConfigSyncTargets
import me.simpleHook.data.config.ConfigWriteCoordinator
import me.simpleHook.data.local.db.AppDatabase
import me.simpleHook.data.local.db.AppRepository
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.feature.backup.domain.BackupHelper
import me.simpleHook.feature.backup.domain.BackupContents
import java.util.UUID

data class ConfigWriteResult(val databaseCommitted: Boolean, val error: Exception? = null) {
    val isSuccess: Boolean get() = databaseCommitted && error == null
}

class AppConfigViewModel(application: Application) : AndroidViewModel(application) {
    var editingConfigId: Int? = null
        private set
    private var editingExtensionConfigId: Int? = null
    private data class RestoreOperation(val source: String, val result: Deferred<ConfigWriteResult>)
    private var restoreOperation: RestoreOperation? = null
    private val committedRestoreSources = mutableSetOf<String>()

    private val database = AppDatabase.getDatabase(application)
    private val configDao = database.getAppConfigDao()
    private val extensionConfigDao = database.getExtensionConfigDao()
    private val appRepository = AppRepository(application)

    val backupLocalWorkerID = MutableLiveData<UUID>()
    val backupCloudWorkerID = MutableLiveData<UUID>()

    // Capture mutable UI entities before scheduling. All writes share one application-wide lock.
    fun saveConfig(config: AppConfig): Deferred<ConfigWriteResult> {
        val snapshot = config.copy()
        return write {
            snapshot.id = editingConfigId ?: snapshot.id
            val previous = if (snapshot.id == 0) emptyList() else configDao.getPackageNamesByIds(listOf(snapshot.id))
            if (snapshot.id == 0) {
                editingConfigId = configDao.insertConfigs(snapshot).single().toInt()
            } else {
                configDao.updateConfigs(snapshot)
                editingConfigId = snapshot.id
            }
            ConfigSyncTargets(custom = previous.associateWith { null } + (snapshot.packageName to editingConfigId))
        }
    }

    fun retrySync(): Deferred<ConfigWriteResult> = viewModelScope.async { syncPending() }

    private suspend fun syncPending(): ConfigWriteResult = try {
        ConfigWriteCoordinator.write { ConfigWriteCoordinator.syncLocked(getApplication()) }
        ConfigWriteResult(databaseCommitted = true)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        reportFailure(error, committed = true)
    }

    fun insertConfigs(vararg appConfig: AppConfig): Deferred<ConfigWriteResult> {
        val copies = appConfig.map { it.copy() }
        return write {
            val ids = configDao.insertConfigs(*copies.toTypedArray())
            ConfigSyncTargets(custom = copies.zip(ids).associate { (config, id) -> config.packageName to id.toInt() })
        }
    }

    fun updateConfigs(vararg appConfig: AppConfig): Deferred<ConfigWriteResult> {
        val copies = appConfig.map { it.copy() }
        return write {
            val previous = configDao.getPackageNamesByIds(copies.map { it.id })
            configDao.updateConfigs(*copies.toTypedArray())
            ConfigSyncTargets(custom = previous.associateWith { null } + copies.associate { it.packageName to it.id })
        }
    }

    fun reorderConfigs(configs: List<AppConfig>, idMapping: Map<Int, Int>): Deferred<ConfigWriteResult> {
        val copies = configs.map { it.copy() }
        val remapping = idMapping.toMap()
        return write(syncFiles = false) {
            // Cancellation must not leave committed rows with stale pending sync IDs.
            withContext(NonCancellable) {
                configDao.updateConfigs(*copies.toTypedArray())
                ConfigWriteCoordinator.remapPendingCustomIdsLocked(remapping)
            }
            ConfigSyncTargets()
        }
    }

    fun deleteConfigs(vararg appConfig: AppConfig): Deferred<ConfigWriteResult> {
        val copies = appConfig.map { it.copy() }
        return write {
            configDao.deleteConfigs(*copies.toTypedArray())
            ConfigSyncTargets(custom = copies.associate { it.packageName to null })
        }
    }

    fun saveExtConfig(config: ExtensionConfigEntity): Deferred<ConfigWriteResult> {
        val snapshot = config.copy()
        return write {
            snapshot.id = editingExtensionConfigId ?: snapshot.id
            val previous = if (snapshot.id == 0) emptyList() else extensionConfigDao.getPackageNamesByIds(listOf(snapshot.id))
            if (snapshot.id == 0) {
                editingExtensionConfigId = extensionConfigDao.insertExtConfigs(snapshot).single().toInt()
            } else {
                extensionConfigDao.updateExtConfigs(snapshot)
                editingExtensionConfigId = snapshot.id
            }
            ConfigSyncTargets(extension = previous.associateWith { null } + (snapshot.packageName to editingExtensionConfigId))
        }
    }

    fun insertExtConfigs(vararg configs: ExtensionConfigEntity): Deferred<ConfigWriteResult> {
        val copies = configs.map { it.copy() }
        return write {
            val ids = extensionConfigDao.insertExtConfigs(*copies.toTypedArray())
            ConfigSyncTargets(extension = copies.zip(ids).associate { (config, id) -> config.packageName to id.toInt() })
        }
    }

    fun deleteExtConfigs(vararg configs: ExtensionConfigEntity): Deferred<ConfigWriteResult> {
        val copies = configs.map { it.copy() }
        return write {
            extensionConfigDao.deleteExtConfigs(*copies.toTypedArray())
            ConfigSyncTargets(extension = copies.associate { it.packageName to null })
        }
    }

    fun deleteExtConfigsByPackageName(packageName: String) = write {
        extensionConfigDao.deleteExtConfigsByPackageName(packageName)
        ConfigSyncTargets(extension = mapOf(packageName to null))
    }

    // The entire restore belongs to the ViewModel, including parsing and retries across rotation.
    fun restore(source: String, read: suspend () -> BackupContents): Deferred<ConfigWriteResult> {
        val previous = restoreOperation?.takeUnless { it.result.isCancelled }
        if (previous?.result?.isActive == true) {
            return if (previous.source == source) previous.result else CompletableDeferred(
                reportFailure(IllegalStateException("Another restore is still running"), committed = false)
            )
        }
        val operation = viewModelScope.async {
            if (source in committedRestoreSources) {
                return@async syncPending()
            }
            val contents = try {
                withContext(Dispatchers.IO) { read() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                return@async reportFailure(error, committed = false)
            }
            val custom = contents.custom.map { it.copy(id = 0) }
            val extension = contents.extension.map { it.copy(id = 0) }
            val collections = contents.collections.map { it.copy(id = 0) }
            commit {
                database.withTransaction {
                    val customIds = configDao.insertConfigs(*custom.toTypedArray())
                    val extensionIds = extensionConfigDao.insertExtConfigs(*extension.toTypedArray())
                    database.getCollectionDao().insertCollections(*collections.toTypedArray())
                    ConfigSyncTargets(
                        custom = custom.zip(customIds).associate { (config, id) -> config.packageName to id.toInt() },
                        extension = extension.zip(extensionIds).associate { (config, id) -> config.packageName to id.toInt() }
                    )
                }
            }.also {
                if (it.databaseCommitted) committedRestoreSources += source
            }
        }
        restoreOperation = RestoreOperation(source, operation)
        return operation
    }

    fun acknowledgeRestore(source: String) {
        committedRestoreSources -= source
        if (restoreOperation?.source == source) restoreOperation = null
    }

    private fun write(
        syncFiles: Boolean = true,
        mutation: suspend () -> ConfigSyncTargets
    ): Deferred<ConfigWriteResult> = viewModelScope.async { commit(syncFiles, mutation) }

    private suspend fun commit(
        syncFiles: Boolean = true,
        mutation: suspend () -> ConfigSyncTargets
    ): ConfigWriteResult {
        // The ViewModel owns saves; a view awaiting the result may disappear during rotation.
        var committed = false
        return try {
            ConfigWriteCoordinator.write {
                val targets = mutation()
                committed = true
                try {
                    if (syncFiles) ConfigWriteCoordinator.syncLocked(getApplication(), targets)
                } finally {
                    // Schedule once for the committed DB state, including partial external failure.
                    notifyBackupConfig()
                }
            }
            ConfigWriteResult(databaseCommitted = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportFailure(error, committed)
        }
    }

    private fun reportFailure(error: Exception, committed: Boolean): ConfigWriteResult {
        LogUtil.outLog(error.stackTraceToString())
        val message = if (committed) R.string.config_sync_partial else R.string.common_failed
        getApplication<Application>().showToast(getApplication<Application>().getString(message))
        return ConfigWriteResult(databaseCommitted = committed, error = error)
    }

    private fun notifyBackupConfig() {
        if (GlobalValue.sp.backup_local_auto) {
            val tag = UUID.randomUUID()
            BackupHelper.localBackupConfig(getApplication(), tag)
            backupLocalWorkerID.value = tag
        }
        if (GlobalValue.sp.backup_cloud_auto) {
            val tag = UUID.randomUUID()
            BackupHelper.cloudBackupConfig(getApplication(), tag)
            backupCloudWorkerID.value = tag
        }
    }

    fun getAllConfigs() = appRepository.getAllConfigs()
    suspend fun getConfigs() = appRepository.getConfigs()
    suspend fun getAllPackageNames() = appRepository.getAllPackageNames()
    fun getAllExtConfigs() = extensionConfigDao.getAllExtConfigs()
    suspend fun getExtConfigs() = extensionConfigDao.getExtConfigs()
    suspend fun getAllExtPackageNames() = extensionConfigDao.getAllExtPackageNames()
    suspend fun getEnabledPackageNames() = appRepository.getEnabledPackageNames()
}
