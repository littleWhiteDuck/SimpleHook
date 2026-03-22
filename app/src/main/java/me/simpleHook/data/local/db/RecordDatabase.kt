package me.simpleHook.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.simpleHook.data.local.db.dao.RecordDao
import me.simpleHook.data.local.db.entity.RecordEntity

@Database(
    entities = [RecordEntity::class],
    version = 3,
    exportSchema = false
)
abstract class RecordDatabase: RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {

        @Volatile
        private var INSTANCE: RecordDatabase? = null

        private const val RECORD_DB_NAME = "records.db"

        @Synchronized
        fun getDatabase(context: Context) = INSTANCE ?: Room.databaseBuilder(
            context.applicationContext, RecordDatabase::class.java, name = RECORD_DB_NAME
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(FTS_CALLBACK)
            .build().also {
            INSTANCE = it
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
            override fun migrate(db: SupportSQLiteDatabase) {
                createRecordFtsArtifacts(db)
            }
        }

        private val FTS_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                createRecordFtsArtifacts(db)
            }
        }

        private fun createRecordFtsArtifacts(db: SupportSQLiteDatabase) {
            val createFts5 =
                "CREATE VIRTUAL TABLE IF NOT EXISTS RecordEntityFts USING fts5(record, content='RecordEntity', content_rowid='id', tokenize='unicode61')"
            val createFts4 =
                "CREATE VIRTUAL TABLE IF NOT EXISTS RecordEntityFts USING fts4(record, content='RecordEntity')"
            runCatching {
                db.execSQL(createFts5)
            }.onFailure {
                db.execSQL(createFts4)
            }

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS record_entity_fts_ai
                AFTER INSERT ON RecordEntity
                BEGIN
                    INSERT INTO RecordEntityFts(rowid, record) VALUES (new.id, new.record);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS record_entity_fts_ad
                AFTER DELETE ON RecordEntity
                BEGIN
                    DELETE FROM RecordEntityFts WHERE rowid = old.id;
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS record_entity_fts_au
                AFTER UPDATE OF record ON RecordEntity
                BEGIN
                    DELETE FROM RecordEntityFts WHERE rowid = old.id;
                    INSERT INTO RecordEntityFts(rowid, record) VALUES (new.id, new.record);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO RecordEntityFts(rowid, record)
                SELECT id, record FROM RecordEntity
                """.trimIndent()
            )
        }
    }
}
