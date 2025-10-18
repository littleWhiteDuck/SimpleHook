package me.simpleHook.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.simpleHook.data.RecordPart
import me.simpleHook.database.entity.RecordEntity


@Dao
interface RecordDao {
    @Query("SELECT * FROM RecordEntity ORDER BY ID DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Insert
    suspend fun insertRecords(vararg recordEntity: RecordEntity)

    @Update
    suspend fun updateRecords(vararg recordEntity: RecordEntity)

    @Delete
    suspend fun deleteRecords(vararg recordEntity: RecordEntity)


    @Query("SELECT * FROM RecordEntity WHERE id = :id")
    fun getRecordById(id: Int): RecordEntity

    @Query("SELECT packageName,type FROM RecordEntity")
    suspend fun getAllRecordPart(): List<RecordPart>

    @Query("SELECT record FROM RecordEntity WHERE type like :type and isMark = 1 ORDER BY time DESC")
    fun getMarkedRecordByType(type: String): List<String>

    @Query("SELECT record FROM RecordEntity WHERE packageName = :packageName and isMark = 1 ORDER BY time DESC")
    fun getMarkedRecordByPack(packageName: String): List<String>

    @Query("SELECT * FROM RecordEntity WHERE packageName = :packageName and record like :pattern ORDER BY time DESC")
    fun getRecordByPack(packageName: String, pattern: String): PagingSource<Int, RecordEntity>

    @Query("SELECT * FROM RecordEntity WHERE type like :type and record like :pattern ORDER BY time DESC")
    fun getRecordByType(type: String, pattern: String): PagingSource<Int, RecordEntity>

    @Query("Delete FROM RecordEntity WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM RecordEntity")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead")
    suspend fun deleteReadRecord(isRead: Boolean)

    @Query("DELETE FROM RecordEntity WHERE time BETWEEN :start and :end")
    suspend fun deleteRecordByTimeRange(start: String, end: String)

    @Query("DELETE FROM RecordEntity WHERE type like :type")
    suspend fun deleteRecordByType(type: String)

    @Query("DELETE FROM RecordEntity WHERE packageName = :packageName")
    suspend fun deleteRecordByPack(packageName: String)

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead and type like :type")
    suspend fun deleteReadRecordByType(isRead: Boolean, type: String)

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead and packageName = :packageName")
    suspend fun deleteReadRecordByPack(isRead: Boolean, packageName: String)

    @Query("DELETE FROM RecordEntity WHERE isMark = :isMark and type like :type")
    suspend fun deleteMarkedRecordByType(isMark: Boolean, type: String)

    @Query("DELETE FROM RecordEntity WHERE isMark = :isMark and packageName = :packageName")
    suspend fun deleteMarkedRecordByPack(isMark: Boolean, packageName: String)
}