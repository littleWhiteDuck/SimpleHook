package me.simpleHook.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
    // appConfig
    fun insertConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.insertConfigs(*appConfig)
    }

    fun updateConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.updateConfigs(*appConfig)
    }

    fun deleteConfigs(vararg appConfig: AppConfig) = viewModelScope.launch {
        appRepository.deleteConfigs(*appConfig)
    }

    fun deleteAllConfigs() = viewModelScope.launch {
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

    fun updateRecord(printLog: PrintLog) = viewModelScope.launch {
        appRepository.updateRecord(printLog)
    }

    fun deleteAllLogs() = viewModelScope.launch {
        appRepository.deleteAllLogs()
    }

    fun deleteHaveReadRecord() = viewModelScope.launch {
        appRepository.deleteHaveReadRecord()
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

    fun deleteAllAssistConfigs() {
        appRepository.deleteAllAssistConfigs()
    }

    fun getAllAssistConfigs() = appRepository.getAllAssistConfigs()

    fun getFilterAssistConfigs(pattern: String) = appRepository.getFilterAssistConfigs(pattern)

}