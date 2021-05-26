package me.simpleHook.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    fun insertConfigs(vararg appConfigEntity: AppConfigEntity) {
        appRepository.insertConfigs(*appConfigEntity)
    }

    fun updateConfigs(vararg appConfigEntity: AppConfigEntity) {
        appRepository.updateConfigs(*appConfigEntity)
    }

    fun deleteConfigs(vararg appConfigEntity: AppConfigEntity) {
        appRepository.deleteConfigs(*appConfigEntity)
    }

    fun deleteAllConfigs() {
        appRepository.deleteAllConfigs()
    }

    fun getAllConfigs() = appRepository.getAllConfigs()
}