package me.simpleHook.feature.config.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.simpleHook.data.local.db.AppRepository
import me.simpleHook.data.local.db.entity.CollectionEntity

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)

    fun insertCollections(vararg collectionEntity: CollectionEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.insertCollections(*collectionEntity)
        }

    fun updateCollections(vararg collectionEntity: CollectionEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.updateCollections(*collectionEntity)
        }

    fun deleteCollections(vararg collectionEntity: CollectionEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.deleteCollections(*collectionEntity)
        }

    fun deleteAllCollections() = viewModelScope.launch(Dispatchers.IO) {
        appRepository.deleteAllCollections()
    }

    fun getAllCollections(): LiveData<List<CollectionEntity>> {
        return appRepository.getAllCollections()
    }

    fun getCollections(): List<CollectionEntity> {
        return appRepository.getCollections()
    }
}
