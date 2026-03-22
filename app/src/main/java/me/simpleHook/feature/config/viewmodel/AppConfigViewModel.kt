package me.simpleHook.feature.config.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.core.GlobalValue
import me.simpleHook.data.config.ConfigSystemUtil
import me.simpleHook.core.constant.Constant
import me.simpleHook.data.local.db.AppDatabase
import me.simpleHook.data.local.db.AppRepository
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import me.simpleHook.platform.lsposed.LSPosedHelper
import me.simpleHook.core.utils.FlavorUtil
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

        appConfig.forEach(::writeToExternal)
        syncScopeForPackages(appConfig.map { it.packageName })

        notifyBackupConfig()
    }


    private fun writeToExternal(appConfig: AppConfig) {
        val configStr = Json.encodeToString(appConfig)
        configSystem.saveCustomConfig(appConfig.packageName, configStr)
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
        viewModelScope.launch {

            appRepository.updateConfigs(*appConfig)

            if (needWriteToFile) {
                appConfig.forEach(::writeToExternal)
                syncScopeForPackages(appConfig.map { it.packageName })
            }

            notifyBackupConfig()
        }

    fun deleteConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.deleteConfigs(*appConfig)

        appConfig.forEach { config ->
            configSystem.deleteCustomConfig(config.packageName)
        }

        syncScopeForPackages(appConfig.map { it.packageName })


        notifyBackupConfig()
    }

    fun getAllConfigs() = appRepository.getAllConfigs()
    fun getConfigs() = appRepository.getConfigs()

    fun getAllPackageNames() = appRepository.getAllPackageNames()


    private fun writeToExternal(extConfigEntity: ExtensionConfigEntity) {
        if (extConfigEntity.packageName == Constant.MODEL_EXTENSION_CONFIG) return
        configSystem.saveExConfig(extConfigEntity.packageName, extConfigEntity.config)
    }

    fun insertExtConfigs(vararg extConfigEntity: ExtensionConfigEntity) =
        viewModelScope.launch(
            Dispatchers.IO
        ) {
            extensionConfigDao.insertExtConfigs(*extConfigEntity)
            extConfigEntity.forEach(::writeToExternal)
            syncScopeForPackages(extConfigEntity.map { it.packageName })
            notifyBackupConfig()
        }

    fun updateExtConfigs(vararg extConfigEntity: ExtensionConfigEntity) =
        viewModelScope.launch {
            extensionConfigDao.updateExtConfigs(*extConfigEntity)
            extConfigEntity.forEach(::writeToExternal)
            syncScopeForPackages(extConfigEntity.map { it.packageName })
            notifyBackupConfig()
        }

    fun deleteExtConfigs(vararg extConfigEntity: ExtensionConfigEntity) =
        viewModelScope.launch {
            extensionConfigDao.deleteExtConfigs(*extConfigEntity)
            syncScopeForPackages(extConfigEntity.map { it.packageName })
            notifyBackupConfig()
        }


    fun deleteExtConfigsByPackageName(packageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            extensionConfigDao.deleteExtConfigsByPackageName(packageName)
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
