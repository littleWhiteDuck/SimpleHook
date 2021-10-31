package me.simpleHook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.simpleHook.database.dao.AppConfigDao
import me.simpleHook.database.dao.AssistConfigDao
import me.simpleHook.database.dao.PrintLogDao
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.PrintLog

@Database(entities = [AppConfig::class, PrintLog::class, AssistConfig::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getAppConfigDao(): AppConfigDao

    abstract fun getLogDao(): PrintLogDao

    abstract fun getAssistConfigDao(): AssistConfigDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getDatabase(context: Context) = instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app_configs.db")
            .build().also {
            instance = it
        }
    }
}