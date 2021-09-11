package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import me.simpleHook.database.entity.PrintLog

@Dao
interface PrintLogDao {

    @Query("DELETE FROM PrintLog")
    suspend fun deleteAllLogs()

    @Query("SELECT * FROM PrintLog ORDER BY ID DESC")
    fun queryAllLogs(): LiveData<List<PrintLog>>
}