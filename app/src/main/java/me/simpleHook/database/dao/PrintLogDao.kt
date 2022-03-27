package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.simpleHook.bean.RecordBean
import me.simpleHook.database.entity.PrintLog

@Dao
interface PrintLogDao {

    @Query("DELETE FROM PrintLog")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM PrintLog WHERE type like :type")
    suspend fun deleteRecordByType(type: String)

    @Query("DELETE FROM PrintLog WHERE packageName = :packageName")
    suspend fun deleteRecordByPack(packageName: String)

    @Query("DELETE FROM PrintLog WHERE read = :read")
    suspend fun deleteReadRecord(read: Int)

    @Query("DELETE FROM PrintLog WHERE read = :read and type like :type")
    suspend fun deleteReadRecordByType(read: Int, type: String)

    @Query("DELETE FROM PrintLog WHERE read = :read and packageName = :packageName")
    suspend fun deleteReadRecordByPack(read: Int, packageName: String)

    @Query("SELECT * FROM PrintLog ORDER BY ID DESC")
    fun queryAllLogs(): LiveData<List<PrintLog>>

    @Query("SELECT * FROM PrintLog ORDER BY ID DESC")
    suspend fun getAllLogs(): List<PrintLog>

    @Query("SELECT * FROM PrintLog WHERE packageName like :pattern or log like :pattern ORDER BY ID DESC")
    suspend fun filterRecord(pattern: String): List<PrintLog>

    @Query("SELECT packageName,type FROM PrintLog")
    suspend fun getAllRecord(): List<RecordBean>

    @Query("SELECT * FROM PrintLog WHERE packageName = :packageName and log like :pattern ORDER BY time DESC")
    suspend fun filterRecordByPack(packageName: String, pattern: String): List<PrintLog>

    @Query("SELECT * FROM PrintLog WHERE type like :type and log like :pattern ORDER BY time DESC")
    suspend fun filterRecordByType(type: String, pattern: String): List<PrintLog>

    @Query("SELECT * FROM PrintLog WHERE packageName = :packageName ORDER BY time DESC")
    fun getRecordByPack(packageName: String): PagingSource<Int, PrintLog>

    @Query("SELECT * FROM PrintLog WHERE type like :type ORDER BY time DESC")
    fun getRecordByType(type: String): PagingSource<Int, PrintLog>

    @Update
    suspend fun updateRecord(printLog: PrintLog)

    @Insert
    suspend fun insertRecord(vararg printLog: PrintLog)
}