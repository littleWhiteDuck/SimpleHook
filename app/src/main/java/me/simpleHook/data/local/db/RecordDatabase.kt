package me.simpleHook.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.simpleHook.data.local.db.dao.RecordDao
import me.simpleHook.data.local.db.entity.RecordEntity

@Database(
    entities = [RecordEntity::class],
    version = 5,
    exportSchema = false
)
abstract class RecordDatabase: RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        private val REQUIRED_RECORD_COLUMNS = setOf(
            "id",
            "type",
            "subType",
            "record",
            "packageName",
            "isRead",
            "isMark",
            "time"
        )

        private val REQUIRED_RECORD_COLUMNS_V5 = REQUIRED_RECORD_COLUMNS + "processName"

        @Volatile
        private var INSTANCE: RecordDatabase? = null

        private const val RECORD_DB_NAME = "records.db"

        @Synchronized
        fun getDatabase(context: Context): RecordDatabase {
            INSTANCE?.let { return it }

            val appContext = context.applicationContext
            repairInvalidDatabaseIfNeeded(appContext)
            cleanupLegacySearchArtifactsIfNeeded(appContext)

            return Room.databaseBuilder(
                appContext,
                RecordDatabase::class.java,
                RECORD_DB_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also {
                    INSTANCE = it
                }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_RecordEntity_packageName_time ON RecordEntity(packageName, time)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_RecordEntity_type_time ON RecordEntity(type, time)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_RecordEntity_isMark_packageName_time ON RecordEntity(isMark, packageName, time)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_RecordEntity_isMark_type_time ON RecordEntity(isMark, type, time)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                dropLegacySearchArtifacts(db::execSQL)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE RecordEntity ADD COLUMN processName TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "UPDATE RecordEntity SET processName = packageName WHERE processName = ''"
                )
            }
        }

        private fun cleanupLegacySearchArtifactsIfNeeded(context: Context) {
            val dbFile = context.getDatabasePath(RECORD_DB_NAME)
            if (!dbFile.exists()) return

            runCatching {
                SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                ).use { db ->
                    if (hasLegacySearchArtifacts(db)) {
                        dropLegacySearchArtifacts(db::execSQL)
                    }
                }
            }
        }

        private fun repairInvalidDatabaseIfNeeded(context: Context) {
            val dbFile = context.getDatabasePath(RECORD_DB_NAME)
            if (!dbFile.exists()) return

            val isValid = runCatching {
                SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).use(::hasValidRecordSchema)
            }.getOrDefault(false)

            if (!isValid) {
                context.deleteDatabase(RECORD_DB_NAME)
            }
        }

        private fun hasValidRecordSchema(db: SQLiteDatabase): Boolean {
            val requiredColumns = if (db.version >= 5) {
                REQUIRED_RECORD_COLUMNS_V5
            } else {
                REQUIRED_RECORD_COLUMNS
            }
            return db.rawQuery("PRAGMA table_info(`RecordEntity`)", null).use { cursor ->
                if (cursor.count == 0) return false

                val nameColumn = cursor.getColumnIndex("name")
                if (nameColumn < 0) return false

                val foundColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    foundColumns += cursor.getString(nameColumn)
                }
                requiredColumns.all(foundColumns::contains)
            }
        }

        private fun hasLegacySearchArtifacts(db: SQLiteDatabase): Boolean {
            return db.rawQuery(
                """
                SELECT 1
                FROM sqlite_master
                WHERE name IN (
                    'RecordEntityFts',
                    'record_entity_fts_ai',
                    'record_entity_fts_ad',
                    'record_entity_fts_au'
                )
                LIMIT 1
                """.trimIndent(),
                null
            ).use { cursor ->
                cursor.moveToFirst()
            }
        }

        private fun dropLegacySearchArtifacts(execSql: (String) -> Unit) {
            listOf(
                "DROP TRIGGER IF EXISTS record_entity_fts_ai",
                "DROP TRIGGER IF EXISTS record_entity_fts_ad",
                "DROP TRIGGER IF EXISTS record_entity_fts_au",
                "DROP TABLE IF EXISTS RecordEntityFts"
            ).forEach { sql ->
                runCatching {
                    execSql(sql)
                }
            }
        }
    }
}
