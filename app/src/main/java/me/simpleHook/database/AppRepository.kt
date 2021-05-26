package me.simpleHook.database

import android.content.Context
import android.os.AsyncTask

class AppRepository(context: Context) {
    private val appConfigDao = AppDatabase.getDatabase(context).getAppConfigDao()
    private var allConfigLive = appConfigDao.queryAll()

    fun insertConfigs(vararg appConfigEntity: AppConfigEntity) {
        InsertAsyncTask(appConfigDao).execute(*appConfigEntity)
    }

    fun updateConfigs(vararg appConfigEntity: AppConfigEntity) {
        UpdateAsyncTask(appConfigDao).execute(*appConfigEntity)
    }

    fun deleteConfigs(vararg appConfigEntity: AppConfigEntity) {
        DeleteAsyncTask(appConfigDao).execute(*appConfigEntity)
    }

    fun deleteAllConfigs() {
        DeleteAllAsyncTask(appConfigDao).execute()
    }
    fun getAllConfigs() = allConfigLive

    class InsertAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfigEntity, Unit, Unit>() {
        override fun doInBackground(vararg appConfigEntity: AppConfigEntity) {
            appConfigDao.insertConfigs(*appConfigEntity)
        }
    }

    class UpdateAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfigEntity, Unit, Unit>() {
        override fun doInBackground(vararg appConfigEntity: AppConfigEntity) {
            appConfigDao.updateConfigs(*appConfigEntity)
        }
    }

    class DeleteAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfigEntity, Unit, Unit>() {
        override fun doInBackground(vararg appConfigEntity: AppConfigEntity) {
            appConfigDao.deleteConfigs(*appConfigEntity)
        }
    }

    class DeleteAllAsyncTask(private val appConfigDao: AppConfigDao) :
        AsyncTask<AppConfigEntity, Unit, Unit>() {
        override fun doInBackground(vararg appConfigEntity: AppConfigEntity) {
            appConfigDao.insertConfigs(*appConfigEntity)
        }
    }

}