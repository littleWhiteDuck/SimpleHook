package me.simpleHook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.simpleHook.database.dao.AppConfigDao
import me.simpleHook.database.dao.AssistConfigDao
import me.simpleHook.database.dao.CollectionDao
import me.simpleHook.database.dao.PrintLogDao
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.database.entity.AssistConfig
import me.simpleHook.database.entity.CollectionEntity
import me.simpleHook.database.entity.PrintLog

@Database(entities = [AppConfig::class, PrintLog::class, AssistConfig::class, CollectionEntity::class],
    version = 6,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getAppConfigDao(): AppConfigDao

    abstract fun getLogDao(): PrintLogDao

    abstract fun getAssistConfigDao(): AssistConfigDao

    abstract fun getCollectionDao(): CollectionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("alter table PrintLog add column read INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("alter table PrintLog add column type TEXT NOT NULL DEFAULT 'update'")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("alter table PrintLog add column time TEXT NOT NULL DEFAULT 'update'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("alter table PrintLog add column isMark INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE CollectionEntity(id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, config TEXT NOT NULL, type Text NOT NULL)");
            }
        }
        private var instance: AppDatabase? = null

        @Synchronized
        fun getDatabase(context: Context) =
            instance ?: Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java,
                "app_configs.db").addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4).addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6).build().also {
                    instance = it
                }
    }
}