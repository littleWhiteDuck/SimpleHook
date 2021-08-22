package me.simpleHook.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)

    // appConfig
    fun insertConfigs(vararg appConfig: AppConfig) {
        appRepository.insertConfigs(*appConfig)
    }

    fun updateConfigs(vararg appConfig: AppConfig) {
        appRepository.updateConfigs(*appConfig)
    }

    fun deleteConfigs(vararg appConfig: AppConfig) {
        appRepository.deleteConfigs(*appConfig)
    }

    fun deleteAllConfigs() {
        appRepository.deleteAllConfigs()
    }

    fun getAllConfigs() = appRepository.getAllConfigs()

    fun getFilterConfigs(pattern: String) = appRepository.getFilterConfigs(pattern)

    // logs

    fun getAllLogs() = appRepository.getAllLogs()

    fun deleteAllLogs() {
        appRepository.deleteAllLogs()
    }

    // Assist
    fun insertAssistConfigs(vararg assistConfig: AssistConfig) {
        appRepository.insertAssistConfigs(*assistConfig)
    }

    fun updateAssistConfigs(vararg assistConfig: AssistConfig) {
        appRepository.updateAssistConfigs(*assistConfig)
    }

    fun deleteAssistConfigs(vararg assistConfig: AssistConfig) {
        appRepository.deleteAssistConfigs(*assistConfig)
    }

    fun deleteAllAssistConfigs() {
        appRepository.deleteAllAssistConfigs()
    }

    fun getAllAssistConfigs() = appRepository.getAllAssistConfigs()

    fun getFilterAssistConfigs(pattern: String) = appRepository.getFilterAssistConfigs(pattern)

}