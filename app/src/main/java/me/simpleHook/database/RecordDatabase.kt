package me.simpleHook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.simpleHook.database.dao.RecordDao
import me.simpleHook.database.entity.RecordEntity

@Database(entities = [RecordEntity::class], version = 1)
abstract class RecordDatabase: RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {

        @Volatile
        private var INSTANCE: RecordDatabase? = null

        private const val RECORD_DB_NAME = "records.db"

        @Synchronized
        fun getDatabase(context: Context) = INSTANCE ?: Room.databaseBuilder(
            context.applicationContext, RecordDatabase::class.java, name = RECORD_DB_NAME
        ).build().also {
            INSTANCE = it
        }
    }
}