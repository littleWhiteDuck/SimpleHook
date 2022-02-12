package me.simpleHook.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.PrintLog

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    private val _filterAppConfig = MutableLiveData<List<AppConfig>>()
    val filterAppConfig: LiveData<List<AppConfig>>
        get() = _filterAppConfig
    private var _filterRecord = MutableLiveData<List<PrintLog>>()
    val filterRecord: LiveData<List<PrintLog>> get() = _filterRecord

    private var _filterRecord2 = MutableLiveData<List<PrintLog>>()
    val filterRecord2: LiveData<List<PrintLog>> get() = _filterRecord2

    // appConfig
    fun insertConfigs(vararg appConfig: AppConfig) = viewModelScope.launch(Dispatchers.IO) {
        appRepository.insertConfigs(*appConfig)
    }

    fun updateConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.updateConfigs(*appConfig)
    }

    fun deleteConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.deleteConfigs(*appConfig)
    }

    fun deleteAllConfigs() = viewModelScope.launch(Dispatchers.IO) {
        appRepository.deleteAllConfigs()
    }

    fun getAllConfigs() = appRepository.getAllConfigs()
    fun getConfigs() = appRepository.getConfigs()

    fun getFilterConfigs(pattern: String) =
        viewModelScope.launch { _filterAppConfig.value = appRepository.getFilterConfigs(pattern) }

    // Record
    fun getAllLogs() = appRepository.getAllLogs()

    fun filterRecord(pattern: String) = viewModelScope.launch {
        _filterRecord.value = appRepository.filterRecord("%$pattern%")
    }

    fun filterRecordByPack(packageName: String, pattern: String) = viewModelScope.launch {
        _filterRecord2.value = appRepository.filterRecordByPack(packageName, "%$pattern%")
    }

    fun filterRecordByType(type: String, pattern: String) = viewModelScope.launch {
        _filterRecord2.value = appRepository.filterRecordByType("%$type%", "%$pattern%")
    }

    fun updateRecord(printLog: PrintLog) = viewModelScope.launch {
        appRepository.updateRecord(printLog)
    }

    fun deleteAllLogs() = viewModelScope.launch {
        appRepository.deleteAllLogs()
    }

    fun deleteRecordByType(type: String) = viewModelScope.launch {
        appRepository.deleteRecordByType("%$type%")
    }

    fun deleteRecordByPack(packageName: String) = viewModelScope.launch {
        appRepository.deleteRecordByPack(packageName)
    }

    fun deleteRecordByRead(read: Int = 1) = viewModelScope.launch {
        appRepository.deleteRecordByRead(read)
    }

    fun deleteReadRecordByPack(read: Int = 1, packageName: String) = viewModelScope.launch {
        appRepository.deleteReadRecordByPack(read, packageName)
    }

    fun deleteReadRecordByType(read: Int = 1, type: String) = viewModelScope.launch {
        appRepository.deleteReadRecordByType(read, "%$type%")
    }


    // Assist
    fun insertAssistConfigs(vararg assistConfig: AssistConfig) = viewModelScope.launch {
        appRepository.insertAssistConfigs(*assistConfig)
    }

    fun updateAssistConfigs(vararg assistConfig: AssistConfig) = viewModelScope.launch {
        appRepository.updateAssistConfigs(*assistConfig)
    }

    fun deleteAssistConfigs(vararg assistConfig: AssistConfig) = viewModelScope.launch {
        appRepository.deleteAssistConfigs(*assistConfig)
    }

    suspend fun queryDefaultExConfig() = appRepository.queryDefaultExConfig()

    fun deleteAllAssistConfigs() {
        appRepository.deleteAllAssistConfigs()
    }

    fun getAllAssistConfigs() = appRepository.getAllAssistConfigs()
    fun getAssistConfigs() = appRepository.getAssistConfigs()

    fun getFilterAssistConfigs(pattern: String) = appRepository.getFilterAssistConfigs(pattern)

}