package me.simpleHook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.simpleHook.database.dao.AppConfigDao
import me.simpleHook.database.dao.AssistConfigDao
import me.simpleHook.database.dao.PrintLogDao
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.PrintLog

@Database(entities = [AppConfig::class, PrintLog::class, AssistConfig::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getAppConfigDao(): AppConfigDao

    abstract fun getLogDao(): PrintLogDao

    abstract fun getAssistConfigDao(): AssistConfigDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("create table PrintLog (" +
                        "packageName text not null," +
                        "log text not null," +
                        "id integer primary key autoincrement not null)")
                database.execSQL("create table AssistConfig (" +
                        "packageName text not null," +
                        "config text not null," +
                        "allSwitch integer not null," +
                        "appName text not null," +
                        "id integer primary key autoincrement not null)")
                database.execSQL("alter table AppConfigEntity rename to AppConfig")
            }
        }
        private var instance: AppDatabase? = null

        @Synchronized
        fun getDatabase(context: Context) = instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app_configs.db")
            .addMigrations(MIGRATION_1_2)
            .build().also {
            instance = it
        }
    }
}