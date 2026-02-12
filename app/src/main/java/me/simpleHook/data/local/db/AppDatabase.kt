package me.simpleHook.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.simpleHook.data.local.db.dao.AppConfigDao
import me.simpleHook.data.local.db.dao.CollectionDao
import me.simpleHook.data.local.db.dao.ExtensionConfigDao
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.CollectionEntity
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity

@Database(
    entities = [AppConfig::class, ExtensionConfigEntity::class, CollectionEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getAppConfigDao(): AppConfigDao

    abstract fun getExtensionConfigDao(): ExtensionConfigDao

    abstract fun getCollectionDao(): CollectionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table PrintLog add column read INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table PrintLog add column type TEXT NOT NULL DEFAULT 'update'")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table PrintLog add column time TEXT NOT NULL DEFAULT 'update'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table PrintLog add column isMark INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE CollectionEntity(id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, config TEXT NOT NULL, type Text NOT NULL)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE PrintLog")
                db.execSQL("DROP TABLE AssistConfig")
                db.execSQL("CREATE TABLE ExtensionConfigEntity(id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, appName TEXT NOT NULL, packageName TEXT NOT NULL, config TEXT NOT NULL, enable INTEGER NOT NULL DEFAULT 1)")
            }
        }
        private var instance: AppDatabase? = null

        @Synchronized
        fun getDatabase(context: Context) =
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_configs.db"
            ).addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4).addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6).addMigrations(MIGRATION_6_7).build().also {
                    instance = it
                }
    }
}
