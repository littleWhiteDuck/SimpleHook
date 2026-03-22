package me.simpleHook.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.simpleHook.data.RecordPackageCount
import me.simpleHook.data.RecordTypeCount
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.data.record.SmallRecordEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface RecordDao {
    @Query("SELECT * FROM RecordEntity ORDER BY ID DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Insert
    suspend fun insertRecords(vararg recordEntity: RecordEntity)

    @Update
    suspend fun updateRecords(vararg recordEntity: RecordEntity)

    @Query("UPDATE RecordEntity SET isRead = :isRead WHERE id = :id AND isRead != :isRead")
    suspend fun updateRecordReadById(id: Int, isRead: Boolean)

    @Query("UPDATE RecordEntity SET isMark = :isMark WHERE id = :id AND isMark != :isMark")
    suspend fun updateRecordMarkById(id: Int, isMark: Boolean)

    @Delete
    suspend fun deleteRecords(vararg recordEntity: RecordEntity)


    @Query("SELECT * FROM RecordEntity WHERE id = :id")
    fun getRecordById(id: Int): RecordEntity

    @Query("SELECT packageName, COUNT(*) AS count FROM RecordEntity GROUP BY packageName ORDER BY COUNT(*) DESC")
    suspend fun getRecordCountByPack(): List<RecordPackageCount>

    @Query("SELECT type, COUNT(*) AS count FROM RecordEntity GROUP BY type ORDER BY COUNT(*) DESC")
    suspend fun getRecordCountByType(): List<RecordTypeCount>

    @Query("SELECT record FROM RecordEntity WHERE type = :type and isMark = 1 ORDER BY time DESC")
    fun getMarkedRecordByType(type: String): List<String>

    @Query("SELECT record FROM RecordEntity WHERE packageName = :packageName and isMark = 1 ORDER BY time DESC")
    fun getMarkedRecordByPack(packageName: String): List<String>

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time
        FROM RecordEntity
        WHERE packageName = :packageName
          AND (
            instr(record, :queryText) > 0
            OR instr(record, :fallbackText) > 0
          )
        ORDER BY time DESC
        """
    )
    fun getRecordByPack(
        packageName: String,
        queryText: String,
        fallbackText: String
    ): PagingSource<Int, SmallRecordEntity>

    @Query("SELECT id,type,subType,packageName,isRead,isMark,time FROM RecordEntity WHERE packageName = :packageName ORDER BY time DESC")
    fun getRecordByPackNoPattern(packageName: String): PagingSource<Int, SmallRecordEntity>

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time
        FROM RecordEntity
        WHERE type = :type
          AND (
            instr(record, :queryText) > 0
            OR instr(record, :fallbackText) > 0
          )
        ORDER BY time DESC
        """
    )
    fun getRecordByType(
        type: String,
        queryText: String,
        fallbackText: String
    ): PagingSource<Int, SmallRecordEntity>

    @Query("SELECT id,type,subType,packageName,isRead,isMark,time FROM RecordEntity WHERE type = :type ORDER BY time DESC")
    fun getRecordByTypeNoPattern(type: String): PagingSource<Int, SmallRecordEntity>

    @Query("Delete FROM RecordEntity WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM RecordEntity")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead")
    suspend fun deleteReadRecord(isRead: Boolean)

    @Query("DELETE FROM RecordEntity WHERE time BETWEEN :start and :end")
    suspend fun deleteRecordByTimeRange(start: String, end: String)

    @Query("DELETE FROM RecordEntity WHERE type = :type")
    suspend fun deleteRecordByType(type: String)

    @Query("DELETE FROM RecordEntity WHERE packageName = :packageName")
    suspend fun deleteRecordByPack(packageName: String)

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead and type = :type")
    suspend fun deleteReadRecordByType(isRead: Boolean, type: String)

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead and packageName = :packageName")
    suspend fun deleteReadRecordByPack(isRead: Boolean, packageName: String)

    @Query("DELETE FROM RecordEntity WHERE isMark = :isMark and type = :type")
    suspend fun deleteMarkedRecordByType(isMark: Boolean, type: String)

    @Query("DELETE FROM RecordEntity WHERE isMark = :isMark and packageName = :packageName")
    suspend fun deleteMarkedRecordByPack(isMark: Boolean, packageName: String)
}
