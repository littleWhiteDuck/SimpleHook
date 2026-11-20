package me.simpleHook.feature.config.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler
import me.simpleHook.R
import me.simpleHook.core.extension.showToast
import me.simpleHook.core.utils.LogUtil
import me.simpleHook.data.local.db.AppRepository
import me.simpleHook.data.local.db.entity.CollectionEntity

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    private val errors = CoroutineExceptionHandler { _, error ->
        LogUtil.outLog(error.stackTraceToString())
        application.showToast(application.getString(R.string.common_failed))
    }

    fun insertCollections(vararg collectionEntity: CollectionEntity) =
        viewModelScope.launch(errors) {
            appRepository.insertCollections(*collectionEntity)
        }

    fun updateCollections(vararg collectionEntity: CollectionEntity) =
        viewModelScope.launch(errors) {
            appRepository.updateCollections(*collectionEntity)
        }

    fun deleteCollections(vararg collectionEntity: CollectionEntity) =
        viewModelScope.launch(errors) {
            appRepository.deleteCollections(*collectionEntity)
        }

    suspend fun deleteAllCollectionsNow() = appRepository.deleteAllCollections()

    fun getAllCollections(): LiveData<List<CollectionEntity>> {
        return appRepository.getAllCollections()
    }

    suspend fun getCollections(): List<CollectionEntity> {
        return appRepository.getCollections()
    }
}
