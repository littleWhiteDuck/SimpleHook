package me.simpleHook.database

import android.content.Context
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.PrintLog

class AppRepository(context: Context) {
    private val appConfigDao = AppDatabase.getDatabase(context).getAppConfigDao()
    private val printLogDao = AppDatabase.getDatabase(context).getLogDao()
    private val assistConfigDao = AppDatabase.getDatabase(context).getAssistConfigDao()

    suspend fun insertConfigs(vararg appConfig: AppConfig) {
        appConfigDao.insertConfigs(*appConfig)
    }

    suspend fun updateConfigs(vararg appConfig: AppConfig) {
        appConfigDao.updateConfigs(*appConfig)
    }

    suspend fun deleteConfigs(vararg appConfig: AppConfig) {
        appConfigDao.deleteConfigs(*appConfig)

    }

    fun deleteAllConfigs() {
        appConfigDao.deleteAllConfigs()
    }

    fun getAllConfigs() = appConfigDao.queryAll()
    fun getConfigs() = appConfigDao.getAll()

    suspend fun getFilterConfigs(pattern: String) = appConfigDao.filterConfigs(pattern)

    // Record
    fun getAllLogs() = printLogDao.queryAllLogs()

    suspend fun filterRecord(pattern: String) = printLogDao.filterRecord(pattern)

    suspend fun filterRecordByPack(packageName: String, pattern: String) =
        printLogDao.filterRecordByPack(packageName, pattern)

    suspend fun filterRecordByType(type: String, pattern: String) =
        printLogDao.filterRecordByType(type, pattern)

    suspend fun deleteAllLogs() {
        printLogDao.deleteAllLogs()
    }

    suspend fun deleteRecordByRead(read: Int) {
        printLogDao.deleteReadRecord(read)
    }

    suspend fun deleteRecordByType(type: String) {
        printLogDao.deleteRecordByType(type)
    }

    suspend fun deleteRecordByPack(packageName: String) {
        printLogDao.deleteRecordByPack(packageName)
    }

    suspend fun deleteReadRecordByPack(read: Int, packageName: String) {
        printLogDao.deleteReadRecordByPack(read, packageName)
    }

    suspend fun deleteReadRecordByType(read: Int, type: String) {
        printLogDao.deleteReadRecordByType(read, type)
    }

    suspend fun updateRecord(printLog: PrintLog) {
        printLogDao.updateRecord(printLog)
    }


    // AssistConfig
    suspend fun insertAssistConfigs(vararg config: AssistConfig) {
        assistConfigDao.insertConfigs(*config)
    }

    suspend fun updateAssistConfigs(vararg config: AssistConfig) {
        assistConfigDao.updateConfigs(*config)
    }

    suspend fun deleteAssistConfigs(vararg config: AssistConfig) {
        assistConfigDao.deleteConfigs(*config)
    }

    fun deleteAllAssistConfigs() {
        assistConfigDao.deleteAllConfigs()
    }

    fun getAllAssistConfigs() = assistConfigDao.getAllConfigs()

    fun getFilterAssistConfigs(pattern: String) = assistConfigDao.getFilterConfigs(pattern)

    suspend fun queryDefaultExConfig(): List<AssistConfig> = assistConfigDao.queryDefaultExConfig()
}