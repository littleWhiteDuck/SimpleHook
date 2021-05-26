package me.simpleHook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppConfigEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getAppConfigDao(): AppConfigDao

    companion object {
        private var instance: AppDatabase? = null
        fun getDatabase(context: Context) = instance?.let {
            it
        } ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_configs.db"
        ).build().also {
            instance = it
        }
    }
}