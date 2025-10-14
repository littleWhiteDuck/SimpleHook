package me.simpleHook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.GlobalValue
import me.simpleHook.config.ConfigSystemUtil
import me.simpleHook.constant.Constant
import me.simpleHook.database.AppRepository
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.lsposed.LSPosedHelper
import me.simpleHook.utils.FlavorUtil
import me.simpleHook.worker.BackupHelper
import java.util.UUID

class AppConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val configSystem by lazy { ConfigSystemUtil.getConfigSystem() }

    private val appRepository = AppRepository(application)

    var backupLocalWorkerID = MutableLiveData<UUID>()
    var backupCloudWorkerID = MutableLiveData<UUID>()

    // appConfig
    fun insertConfigs(vararg appConfig: AppConfig) = viewModelScope.launch(Dispatchers.IO) {

        appRepository.insertConfigs(*appConfig)

        appConfig.forEach(::writeToExternal)

        notifyBackupConfig()
    }


    private fun writeToExternal(appConfig: AppConfig) {
        val configStr = Json.encodeToString(appConfig)
        if (FlavorUtil.rootVersion || FlavorUtil.liteVersion) {
            // TODO, importing config may result in repeated requests
            LSPosedHelper.changeScope(appConfig.packageName, appConfig.enable)
        }
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

            if (needWriteToFile) appConfig.forEach(::writeToExternal)

            notifyBackupConfig()
        }

    fun deleteConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.deleteConfigs(*appConfig)

        appConfig.forEach { config ->
            configSystem.deleteCustomConfig(config.packageName)
        }

        if (FlavorUtil.rootVersion) {
            val pkgNames = appConfig.filter { getExCountByPackageName(it.packageName) < 1 }
                .mapTo(HashSet()) { it.packageName }
            LSPosedHelper.removeScope(pkgNames.toTypedArray())
        }


        notifyBackupConfig()
    }


    private suspend fun getCustomCountByPackageName(packageName: String) =
        appRepository.getCustomCountByPackageName(packageName)

    fun getAllConfigs() = appRepository.getAllConfigs()
    fun getConfigs() = appRepository.getConfigs()

    fun getAllPackageNames() = appRepository.getAllPackageNames()


    private fun writeToExternal(assistConfig: AssistConfig) {
        if (assistConfig.packageName == Constant.MODEL_EXTENSION_CONFIG) return
        if (FlavorUtil.rootVersion || FlavorUtil.liteVersion) {
            // TODO, importing config may result in repeated requests
            LSPosedHelper.changeScope(assistConfig.packageName, assistConfig.allSwitch)
        }
        configSystem.saveExConfig(assistConfig.packageName, assistConfig.config)
    }

    fun insertAssistConfigs(vararg assistConfig: AssistConfig) = viewModelScope.launch {
        appRepository.insertAssistConfigs(*assistConfig)
        assistConfig.forEach(::writeToExternal)
        notifyBackupConfig()
    }

    fun updateAssistConfigs(vararg assistConfig: AssistConfig) = viewModelScope.launch {
        appRepository.updateAssistConfigs(*assistConfig)
        assistConfig.forEach(::writeToExternal)
        notifyBackupConfig()
    }

    fun deleteAssistConfigs(vararg assistConfig: AssistConfig) = viewModelScope.launch {
        appRepository.deleteAssistConfigs(*assistConfig)
        if (FlavorUtil.rootVersion || FlavorUtil.liteVersion) {
            val pkgNames = HashSet<String>()
            assistConfig.forEach {
                val count = getCustomCountByPackageName(it.packageName)
                if (count < 1) {
                    pkgNames.add(it.packageName)
                }
            }
            LSPosedHelper.removeScope(pkgNames.toTypedArray())
        }
        notifyBackupConfig()
    }


    fun deleteAssistConfigsByPackageName(packageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteAssistConfigsByPackageName(packageName)
            notifyBackupConfig()
        }

    fun getAllAssistConfigs() = appRepository.getAllAssistConfigs()
    fun getAssistConfigs() = appRepository.getAssistConfigs()

    private suspend fun getExCountByPackageName(packageName: String) =
        appRepository.getExCountByPackageName(packageName)

}