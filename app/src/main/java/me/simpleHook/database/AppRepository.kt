package me.simpleHook.database

import android.content.Context
import androidx.lifecycle.LiveData
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.CollectionEntity
import me.simpleHook.database.entity.PrintLog

class AppRepository(context: Context) {
    private val appConfigDao = AppDatabase.getDatabase(context).getAppConfigDao()
    private val printLogDao = AppDatabase.getDatabase(context).getLogDao()
    private val assistConfigDao = AppDatabase.getDatabase(context).getAssistConfigDao()
    private val collectionDao = AppDatabase.getDatabase(context).getCollectionDao()
    fun getPrintLogDao() = printLogDao
    suspend fun insertConfigs(vararg appConfig: AppConfig) {
        appConfigDao.insertConfigs(*appConfig)
    }

    suspend fun updateConfigs(vararg appConfig: AppConfig) {
        appConfigDao.updateConfigs(*appConfig)
    }

    suspend fun deleteConfigs(vararg appConfig: AppConfig) {
        appConfigDao.deleteConfigs(*appConfig)

    }

    suspend fun getCustomCountByPackageName(packageName: String): Int {
        return appConfigDao.getCountByPackageName(packageName)
    }

    fun deleteAllConfigs() {
        appConfigDao.deleteAllConfigs()
    }

    fun getAllConfigs() = appConfigDao.queryAll()
    fun getConfigs() = appConfigDao.getAll()

    fun getAllPackageNames() = appConfigDao.getAllPackageNames()
    fun getAllExtensionPackageNames() = assistConfigDao.getAllExtensionPackageNames()

    suspend fun getFilterConfigs(pattern: String) = appConfigDao.filterConfigs(pattern)

    // Record
    fun getAllLogs() = printLogDao.queryAllLogs()

    suspend fun filterRecord(pattern: String) = printLogDao.filterRecord(pattern)

    suspend fun getAllRecord() = printLogDao.getAllRecord()

    fun getMarkedByType(type: String) = printLogDao.getMarkedRecordByType(type)

    fun getMarkedByPack(packageName: String) = printLogDao.getMarkedRecordByPack(packageName)

    fun getRecordByID(id: Int) = printLogDao.getRecordByID(id)

    suspend fun filterRecordByPack(packageName: String, pattern: String) =
        printLogDao.filterRecordByPack(packageName, pattern)

    suspend fun filterRecordByType(type: String, pattern: String) =
        printLogDao.filterRecordByType(type, pattern)

    suspend fun deleteAllLogs() {
        printLogDao.deleteAllLogs()
    }

    suspend fun deleteRecordByTimeRange(start: String, end: String) {
        printLogDao.deleteRecordByTimeRange(start, end)
    }

    suspend fun deleteRecordById(id: Int) {
        printLogDao.deleteRecordById(id)
    }

    suspend fun deleteRecordByRead(read: Boolean) {
        printLogDao.deleteReadRecord(read)
    }

    suspend fun deleteRecordByType(type: String) {
        printLogDao.deleteRecordByType(type)
    }

    suspend fun deleteRecordByPack(packageName: String) {
        printLogDao.deleteRecordByPack(packageName)
    }

    suspend fun deleteReadRecordByPack(read: Boolean, packageName: String) {
        printLogDao.deleteReadRecordByPack(read, packageName)
    }

    suspend fun deleteReadRecordByType(read: Boolean, type: String) {
        printLogDao.deleteReadRecordByType(read, type)
    }

    suspend fun deleteMarkedRecordByPack(isMark: Boolean, packageName: String) {
        printLogDao.deleteMarkedRecordByPack(isMark, packageName)
    }

    suspend fun deleteMarkedRecordByType(isMark: Boolean, type: String) {
        printLogDao.deleteMarkedRecordByType(isMark, type)
    }

    suspend fun updateRecord(printLog: PrintLog) {
        printLogDao.updateRecord(printLog)
    }

    suspend fun insertRecord(vararg printLog: PrintLog) {
        printLogDao.insertRecord(*printLog)
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

    fun deleteAssistConfigsByPackageName(packageName: String) {
        assistConfigDao.deleteAssistConfigsByPackageName(packageName)
    }

    fun getAllAssistConfigs() = assistConfigDao.getAllConfigs()
    fun getAssistConfigs() = assistConfigDao.getConfigs()
    fun getFilterAssistConfigs(pattern: String) = assistConfigDao.getFilterConfigs(pattern)

    suspend fun queryDefaultExConfig(): List<AssistConfig> = assistConfigDao.queryDefaultExConfig()

    suspend fun getExCountByPackageName(packageName: String): Int {
        return assistConfigDao.getCountByPackageName(packageName)
    }


    // collection

    fun insertCollections(vararg collectionEntity: CollectionEntity) {
        collectionDao.insertCollections(*collectionEntity)
    }

    fun updateCollections(vararg collectionEntity: CollectionEntity) {
        collectionDao.updateCollections(*collectionEntity)
    }

    fun deleteCollections(vararg collectionEntity: CollectionEntity) {
        collectionDao.deleteCollections(*collectionEntity)
    }

    fun deleteAllCollections() {
        collectionDao.deleteAllCollections()
    }

    fun getAllCollections(): LiveData<List<CollectionEntity>> {
        return collectionDao.queryAll()
    }

    fun getCollections(): List<CollectionEntity> {
        return collectionDao.getAll()
    }
}