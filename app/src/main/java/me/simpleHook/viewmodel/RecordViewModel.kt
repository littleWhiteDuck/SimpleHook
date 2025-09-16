package me.simpleHook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.simpleHook.constant.Constant
import me.simpleHook.data.LogBean
import me.simpleHook.data.Record
import me.simpleHook.database.AppRepository
import me.simpleHook.database.entity.PrintLog

class RecordViewModel(application: Application) : AndroidViewModel(application){
    private val appRepository = AppRepository(application)

    private var _filterRecordPT = MutableLiveData<List<Record>>()
    val filterRecordPT: LiveData<List<Record>> get() = _filterRecordPT
    val queryPattern = MutableStateFlow("")
    private val pagingConfig =
        PagingConfig(pageSize = 30, prefetchDistance = 3, enablePlaceholders = true, maxSize = 200)

    fun getRecord(
        typeOrPack: String, isType: Boolean, searchMode: Int
    ): Flow<PagingData<PrintLog>> {
        return Pager(config = pagingConfig) {
            if (isType) {
                appRepository.getPrintLogDao()
                    .getRecordByType("%$typeOrPack%", "%${queryPattern.value}%")
            } else {
                appRepository.getPrintLogDao()
                    .getRecordByPack(typeOrPack, "%${queryPattern.value}%")
            }
        }.flow.cachedIn(viewModelScope).combine(queryPattern) { pagingData, query ->
            if (query.isBlank()) {
                pagingData
            } else {
                when (searchMode) {
                    Constant.RECORD_SEARCH_RESULT -> {
                        pagingData.filter {
                            val logBean = Json.Default.decodeFromString<LogBean>(it.log)
                            val list: List<String> = logBean.other
                            list.forEach { item ->
                                if (item.startsWith("加密结果") || item.startsWith("解密结果") || item.startsWith(
                                        "Encrypt result"
                                    ) || item.startsWith("Decrypt result")
                                ) {
                                    item.contains(query)
                                    return@filter true
                                }
                            }
                            false
                        }
                    }

                    Constant.RECORD_SEARCH_RAW_DATA -> {
                        pagingData.filter {
                            val logBean = Json.Default.decodeFromString<LogBean>(it.log)
                            val list: List<String> = logBean.other
                            list.forEach { item ->
                                if (item.startsWith("原始数据") || item.startsWith("Raw Data")) {
                                    item.contains(query)
                                    return@filter true
                                }
                            }
                            false
                        }
                    }

                    else -> pagingData
                }
            }
        }
    }

    //combine(queryInit, transform = { printLog, query ->
//                printLog.filter { it.log.contains(query, true) }
//            })
//    fun getAllLogs() = appRepository.getAllLogs()

    fun getMarkedRecordByType(type: String) = appRepository.getMarkedByType("%$type%")

    fun getMarkedRecordByPack(packageName: String) = appRepository.getMarkedByPack(packageName)

    fun getRecordByID(id: Int) = appRepository.getRecordByID(id)

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

}