package me.simpleHook.data.local.db

import android.content.Context
import androidx.lifecycle.LiveData
import me.simpleHook.data.local.db.entity.CollectionEntity

class AppRepository(context: Context) {
    private val appConfigDao = AppDatabase.getDatabase(context).getAppConfigDao()

    private val extConfigDao = AppDatabase.getDatabase(context).getExtensionConfigDao()

    private val collectionDao = AppDatabase.getDatabase(context).getCollectionDao()

    suspend fun getEnabledPackageNames(): Set<String> {
        return appConfigDao.getEnabledPackageNames().toSet()
    }

    fun getAllConfigs() = appConfigDao.queryAll()
    suspend fun getConfigs() = appConfigDao.getAll()

    suspend fun getAllPackageNames() = appConfigDao.getAllPackageNames()

    suspend fun getExtConfigs() = extConfigDao.getExtConfigs()

    // collection

    suspend fun insertCollections(vararg collectionEntity: CollectionEntity) {
        collectionDao.insertCollections(*collectionEntity)
    }

    suspend fun updateCollections(vararg collectionEntity: CollectionEntity) {
        collectionDao.updateCollections(*collectionEntity)
    }

    suspend fun deleteCollections(vararg collectionEntity: CollectionEntity) {
        collectionDao.deleteCollections(*collectionEntity)
    }

    suspend fun deleteAllCollections() {
        collectionDao.deleteAllCollections()
    }

    fun getAllCollections(): LiveData<List<CollectionEntity>> {
        return collectionDao.queryAll()
    }

    suspend fun getCollections(): List<CollectionEntity> {
        return collectionDao.getAll()
    }
}
