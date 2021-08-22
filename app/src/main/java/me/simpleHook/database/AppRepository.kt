package me.simpleHook.database

import android.content.Context
import android.os.AsyncTask
import me.simpleHook.database.dao.AppConfigDao
import me.simpleHook.database.dao.AssistConfigDao
import me.simpleHook.database.dao.PrintLogDao
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.PrintLog

class AppRepository(context: Context) {
    private val appConfigDao = AppDatabase.getDatabase(context).getAppConfigDao()
    private val logDao = AppDatabase.getDatabase(context).getLogDao()
    private val assistConfigDao = AppDatabase.getDatabase(context).getAssistConfigDao()

    fun insertConfigs(vararg appConfig: AppConfig) {
        InsertAsyncTask(appConfigDao).execute(*appConfig)
    }

    fun updateConfigs(vararg appConfig: AppConfig) {
        UpdateAsyncTask(appConfigDao).execute(*appConfig)
    }

    fun deleteConfigs(vararg appConfig: AppConfig) {
        DeleteAsyncTask(appConfigDao).execute(*appConfig)
    }

    fun deleteAllConfigs() {
        DeleteAllAsyncTask(appConfigDao).execute()
    }

    fun getAllConfigs() = appConfigDao.queryAll()

    fun getFilterConfigs(pattern: String) = appConfigDao.filterConfigs(pattern)

    class InsertAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfig, Unit, Unit>() {
        override fun doInBackground(vararg appConfig: AppConfig) {
            appConfigDao.insertConfigs(*appConfig)
        }
    }

    class UpdateAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfig, Unit, Unit>() {
        override fun doInBackground(vararg appConfig: AppConfig) {
            appConfigDao.updateConfigs(*appConfig)
        }
    }

    class DeleteAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfig, Unit, Unit>() {
        override fun doInBackground(vararg appConfig: AppConfig) {
            appConfigDao.deleteConfigs(*appConfig)
        }
    }

    class DeleteAllAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfig, Unit, Unit>() {
        override fun doInBackground(vararg appConfig: AppConfig) {
            appConfigDao.deleteAllConfigs()
        }
    }

    fun getAllLogs() = logDao.queryAllLogs()

    fun deleteAllLogs() {
        DeleteAllLogAsyncTask(logDao).execute()
    }

    class DeleteAllLogAsyncTask(private val printLogDao: PrintLogDao) :
        AsyncTask<PrintLog, Unit, Unit>() {
        override fun doInBackground(vararg printLog: PrintLog) {
            printLogDao.deleteAllLogs()
        }
    }

    // AssistConfig
    fun insertAssistConfigs(vararg config: AssistConfig) {
        InsertAssistConfig(assistConfigDao).execute(*config)
    }

    fun updateAssistConfigs(vararg config: AssistConfig) {
        UpdateAssistConfig(assistConfigDao).execute(*config)
    }

    fun deleteAssistConfigs(vararg config: AssistConfig) {
        DeleteAssistConfig(assistConfigDao).execute(*config)
    }

    fun deleteAllAssistConfigs() {
        DeleteAllAssistConfig(assistConfigDao).execute()
    }

    fun getAllAssistConfigs() = assistConfigDao.getAllConfigs()

    fun getFilterAssistConfigs(pattern: String) = assistConfigDao.getFilterConfigs(pattern)

    class InsertAssistConfig(private val assistConfigDao: AssistConfigDao) :
        AsyncTask<AssistConfig, Unit, Unit>() {
        override fun doInBackground(vararg config: AssistConfig) {
            assistConfigDao.insertConfigs(*config)
        }
    }

    class UpdateAssistConfig(private val assistConfigDao: AssistConfigDao) :
        AsyncTask<AssistConfig, Unit, Unit>() {
        override fun doInBackground(vararg config: AssistConfig) {
            assistConfigDao.updateConfigs(*config)
        }
    }

    class DeleteAssistConfig(private val assistConfigDao: AssistConfigDao) :
        AsyncTask<AssistConfig, Unit, Unit>() {
        override fun doInBackground(vararg config: AssistConfig) {
            assistConfigDao.deleteConfigs(*config)
        }
    }

    class DeleteAllAssistConfig(private val assistConfigDao: AssistConfigDao) :
        AsyncTask<AssistConfig, Unit, Unit>() {
        override fun doInBackground(vararg config: AssistConfig) {
            assistConfigDao.deleteAllConfigs()
        }
    }
}