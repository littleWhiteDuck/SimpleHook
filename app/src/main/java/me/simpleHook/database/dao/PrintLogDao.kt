package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import me.simpleHook.database.entity.PrintLog

@Dao
interface PrintLogDao {

    @Query("DELETE FROM PrintLog")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM PrintLog WHERE read = 1")
    suspend fun deleteHaveReadRecord()

    @Query("SELECT * FROM PrintLog ORDER BY ID DESC")
    fun queryAllLogs(): LiveData<List<PrintLog>>

    @Query("SELECT * FROM PrintLog ORDER BY ID DESC")
    suspend fun getAllLogs(): List<PrintLog>

    @Query("SELECT * FROM PrintLog WHERE packageName like :pattern or log like :pattern ORDER BY ID DESC")
    suspend fun filterRecord(pattern: String): List<PrintLog>

    @Update
    suspend fun updateRecord(printLog: PrintLog)
}