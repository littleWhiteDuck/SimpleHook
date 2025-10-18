package me.simpleHook.database

import android.content.Context
import androidx.lifecycle.LiveData
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.CollectionEntity

class AppRepository(context: Context) {
    private val appConfigDao = AppDatabase.getDatabase(context).getAppConfigDao()

    private val extConfigDao = AppDatabase.getDatabase(context).getExtensionConfigDao()

    private val collectionDao = AppDatabase.getDatabase(context).getCollectionDao()

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

    fun getExtConfigs() = extConfigDao.getExtConfigs()

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