package me.simpleHook.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.simpleHook.bean.LogBean
import me.simpleHook.bean.RecordBean
import me.simpleHook.constant.Constant
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.PrintLog

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    private val _filterAppConfig = MutableLiveData<List<AppConfig>>()
    private var _filterRecordPT = MutableLiveData<List<RecordBean>>()
    val filterRecordPT: LiveData<List<RecordBean>> get() = _filterRecordPT

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

    fun getAllExtensionPackageNames() = appRepository.getAllExtensionPackageNames()
    fun getAllPackageNames() = appRepository.getAllPackageNames()

    fun getFilterConfigs(pattern: String) =
        viewModelScope.launch { _filterAppConfig.value = appRepository.getFilterConfigs(pattern) }

    // Record
    val queryPattern = MutableLiveData("")
    private val pagingConfig = PagingConfig(
        pageSize = 30, prefetchDistance = 3, enablePlaceholders = true, maxSize = 200
    )

    fun getRecord(
        typeOrPack: String, isType: Boolean, searchMode: Int
    ): Flow<PagingData<PrintLog>> {
        return Pager(
            config = pagingConfig
        ) {
            if (isType) {
                appRepository.getPrintLogDao()
                    .getRecordByType("%$typeOrPack%", "%${queryPattern.value}%")
            } else {
                appRepository.getPrintLogDao()
                    .getRecordByPack(typeOrPack, "%${queryPattern.value}%")
            }
        }.flow.cachedIn(viewModelScope).map { pagingData ->
            when (searchMode) {
                Constant.RECORD_SEARCH_RESULT -> {
                    pagingData.filter {
                        val logBean = Gson().fromJson(it.log, LogBean::class.java)
                        val list: List<String> = logBean.other as List<String>
                        var result = ""
                        list.forEach { item ->
                            if (item.startsWith("加密结果") || item.startsWith("解密结果") || item.startsWith(
                                    "Encrypt result"
                                ) || item.startsWith("Decrypt result")
                            ) {
                                result = item
                                return@forEach
                            }
                        }
                        result.contains(queryPattern.value ?: "")
                    }
                }
                Constant.RECORD_SEARCH_RAW_DATA -> {
                    pagingData.filter {
                        val logBean = Gson().fromJson(it.log, LogBean::class.java)
                        val list: List<String> = logBean.other as List<String>
                        var rawData = ""
                        list.forEach { item ->
                            if (item.startsWith("原始数据") || item.startsWith("Raw Data")) {
                                rawData = item
                                return@forEach
                            }
                        }
                        rawData.contains(queryPattern.value ?: "")
                    }
                }
                else -> pagingData
            }
        }
    }

    //combine(queryInit, transform = { printLog, query ->
//                printLog.filter { it.log.contains(query, true) }
//            })
    fun getAllLogs() = appRepository.getAllLogs()

    fun getMarkedRecordByType(type: String) = appRepository.getMarkedByType("%$type%")

    fun getMarkedRecordByPack(packageName: String) = appRepository.getMarkedByPack(packageName)

    fun getAllRecord() = viewModelScope.launch {
        _filterRecordPT.value = appRepository.getAllRecord()
    }


    fun updateRecord(printLog: PrintLog) = viewModelScope.launch {
        appRepository.updateRecord(printLog)
    }

    fun insertRecord(vararg printLog: PrintLog) = viewModelScope.launch {
        appRepository.insertRecord(*printLog)
    }

    fun deleteAllLogs() = viewModelScope.launch {
        appRepository.deleteAllLogs()
    }

    fun deleteRecordByTimeRange(start: String, end: String) = viewModelScope.launch {
        appRepository.deleteRecordByTimeRange(start, end)
    }

    fun deleteRecordById(id: Int) = viewModelScope.launch {
        appRepository.deleteRecordById(id)
    }

    fun deleteRecordByType(type: String) = viewModelScope.launch {
        appRepository.deleteRecordByType("%$type%")
    }

    fun deleteRecordByPack(packageName: String) = viewModelScope.launch {
        appRepository.deleteRecordByPack(packageName)
    }

    fun deleteRecordByRead(read: Boolean) = viewModelScope.launch {
        appRepository.deleteRecordByRead(read)
    }

    fun deleteReadRecordByPack(read: Boolean, packageName: String) = viewModelScope.launch {
        appRepository.deleteReadRecordByPack(read, packageName)
    }

    fun deleteReadRecordByType(read: Boolean, type: String) = viewModelScope.launch {
        appRepository.deleteReadRecordByType(read, "%$type%")
    }

    fun deleteMarkedRecordByPack(isMark: Boolean, packageName: String) = viewModelScope.launch {
        appRepository.deleteMarkedRecordByPack(isMark, packageName)
    }

    fun deleteMarkedRecordByType(isMark: Boolean, type: String) = viewModelScope.launch {
        appRepository.deleteMarkedRecordByType(isMark, "%$type%")
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

    fun deleteAssistConfigsByPackageName(packageName: String) {
        appRepository.deleteAssistConfigsByPackageName(packageName)
    }

    fun getAllAssistConfigs() = appRepository.getAllAssistConfigs()
    fun getAssistConfigs() = appRepository.getAssistConfigs()

    fun getFilterAssistConfigs(pattern: String) = appRepository.getFilterAssistConfigs(pattern)

}