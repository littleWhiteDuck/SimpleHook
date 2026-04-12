package me.simpleHook.feature.config.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.core.GlobalValue
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.data.config.ConfigSystemUtil
import me.simpleHook.data.config.ConfigRemoteSyncHelper
import me.simpleHook.data.local.db.AppDatabase
import me.simpleHook.data.local.db.AppRepository
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.platform.lsposed.LSPosedHelper
import me.simpleHook.feature.backup.domain.BackupHelper
import java.util.UUID

class AppConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val extensionConfigDao = AppDatabase.getDatabase(application).getExtensionConfigDao()

    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }

    private val appRepository = AppRepository(application)

    var backupLocalWorkerID = MutableLiveData<UUID>()
    var backupCloudWorkerID = MutableLiveData<UUID>()

    // appConfig
    fun insertConfigs(vararg appConfig: AppConfig) = viewModelScope.launch(Dispatchers.IO) {

        appRepository.insertConfigs(*appConfig)

        for (config in appConfig) {
            writeToExternal(config)
        }
        syncScopeForPackages(appConfig.map { it.packageName })

        notifyBackupConfig()
    }


    private suspend fun writeToExternal(appConfig: AppConfig) {
        val configStr = Json.encodeToString(appConfig)
        ConfigRemoteSyncHelper.saveCustomConfig(configSystem, appConfig.packageName, configStr)
    }

    private fun notifyBackupConfig() = viewModelScope.launch(Dispatchers.Main) {
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

    fun updateConfigs(vararg appConfig: AppConfig, needWriteToFile: Boolean = true) =
        viewModelScope.launch(Dispatchers.IO) {

            appRepository.updateConfigs(*appConfig)

            if (needWriteToFile) {
                for (config in appConfig) {
                    writeToExternal(config)
                }
                syncScopeForPackages(appConfig.map { it.packageName })
            }

            notifyBackupConfig()
        }

    fun deleteConfigs(vararg appConfig: AppConfig) = viewModelScope.launch(Dispatchers.IO) {
        appRepository.deleteConfigs(*appConfig)

        appConfig.forEach { config ->
            ConfigRemoteSyncHelper.deleteCustomConfig(configSystem, config.packageName)
        }

        syncScopeForPackages(appConfig.map { it.packageName })


        notifyBackupConfig()
    }

    fun getAllConfigs() = appRepository.getAllConfigs()
    fun getConfigs() = appRepository.getConfigs()

    fun getAllPackageNames() = appRepository.getAllPackageNames()


    private suspend fun writeToExternal(extConfigEntity: ExtensionConfigEntity) {
        if (extConfigEntity.packageName == Constant.MODEL_EXTENSION_CONFIG) return
        ConfigRemoteSyncHelper.saveExtensionConfig(
            configSystem,
            extConfigEntity.packageName,
            extConfigEntity.config
        )
    }

    fun insertExtConfigs(vararg extConfigEntity: ExtensionConfigEntity) =
        viewModelScope.launch(
            Dispatchers.IO
        ) {
            extensionConfigDao.insertExtConfigs(*extConfigEntity)
            for (config in extConfigEntity) {
                writeToExternal(config)
            }
            syncScopeForPackages(extConfigEntity.map { it.packageName })
            notifyBackupConfig()
        }

    fun updateExtConfigs(vararg extConfigEntity: ExtensionConfigEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            extensionConfigDao.updateExtConfigs(*extConfigEntity)
            for (config in extConfigEntity) {
                writeToExternal(config)
            }
            syncScopeForPackages(extConfigEntity.map { it.packageName })
            notifyBackupConfig()
        }

    fun deleteExtConfigs(vararg extConfigEntity: ExtensionConfigEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            extensionConfigDao.deleteExtConfigs(*extConfigEntity)
            extConfigEntity.forEach { config ->
                if (config.packageName == Constant.MODEL_EXTENSION_CONFIG) return@forEach
                ConfigRemoteSyncHelper.deleteExtensionConfig(configSystem, config.packageName)
            }
            syncScopeForPackages(extConfigEntity.map { it.packageName })
            notifyBackupConfig()
        }


    fun deleteExtConfigsByPackageName(packageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            extensionConfigDao.deleteExtConfigsByPackageName(packageName)
            if (packageName != Constant.MODEL_EXTENSION_CONFIG) {
                ConfigRemoteSyncHelper.deleteExtensionConfig(configSystem, packageName)
            }
            notifyBackupConfig()
        }

    fun getAllExtConfigs() = extensionConfigDao.getAllExtConfigs()
    fun getExtConfigs() = extensionConfigDao.getExtConfigs()

    /**
     * @return packageName set of custom config + extension config
     */
    suspend fun getEnabledPackageNames() = appRepository.getEnabledPackageNames()

    private suspend fun getEnabledCustomCountByPackageName(packageName: String) =
        appRepository.getEnabledCustomCountByPackageName(packageName)

    private suspend fun getEnabledExCountByPackageName(packageName: String) =
        extensionConfigDao.getEnabledExtCountByPackageName(packageName)

    private suspend fun syncScopeForPackages(packageNames: Collection<String>) {
        if (!FlavorUtil.rootVersion) return
        val packages = packageNames.toSet().filter { it != Constant.MODEL_EXTENSION_CONFIG }
        val enabled = packages.filter {
            getEnabledCustomCountByPackageName(it) > 0 || getEnabledExCountByPackageName(it) > 0
        }
        val disabled = packages - enabled.toSet()
        LSPosedHelper.requestScopes(enabled)
        LSPosedHelper.removeScopes(disabled)
    }

}
