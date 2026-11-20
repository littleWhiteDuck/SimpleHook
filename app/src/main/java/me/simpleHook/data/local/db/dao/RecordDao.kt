package me.simpleHook.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.simpleHook.data.RecordPackageCount
import me.simpleHook.data.RecordTypeCount
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.data.record.SmallRecordEntity
import kotlinx.coroutines.flow.Flow


data class MarkedRecordKey(val id: Int, val time: String)

@Dao
interface RecordDao {
    @Query("""SELECT id, time FROM RecordEntity WHERE isMark = 1 AND id <= :upperBound
        AND (:lastTime IS NULL OR time < :lastTime OR (time = :lastTime AND id < :beforeId))
        AND ((:isType = 1 AND type = :filter) OR (:isType = 0 AND packageName = :filter))
        ORDER BY time DESC, id DESC LIMIT :limit""")
    suspend fun getMarkedIds(filter: String, isType: Boolean, lastTime: String?, beforeId: Int, upperBound: Int, limit: Int): List<MarkedRecordKey>

    @Query("SELECT COALESCE(MAX(id), 0) FROM RecordEntity")
    suspend fun getLatestRecordId(): Int

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta
        FROM (
            SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta
            FROM RecordEntity
            WHERE id > :afterId AND time >= :startTime
            ORDER BY id DESC
            LIMIT :limit
        ) AS recent
        ORDER BY id ASC
        """
    )
    fun getFloatRecordsAfter(
        afterId: Int,
        startTime: String,
        limit: Int
    ): Flow<List<SmallRecordEntity>>

    @Query("DELETE FROM RecordEntity WHERE id > :afterId AND time >= :startTime")
    suspend fun deleteFloatRecordsAfter(afterId: Int, startTime: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(vararg recordEntity: RecordEntity)

    @Query("UPDATE RecordEntity SET isRead = :isRead WHERE id = :id AND isRead != :isRead")
    suspend fun updateRecordReadById(id: Int, isRead: Boolean)

    @Query("UPDATE RecordEntity SET isMark = :isMark WHERE id = :id AND isMark != :isMark")
    suspend fun updateRecordMarkById(id: Int, isMark: Boolean)

    @Query(
        """
        SELECT id,type,subType,'' AS record,packageName,processName,isRead,isMark,time,sourceKey,summaryTitle,summarySubtitle,summaryMeta
        FROM RecordEntity
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getRecordMetaById(id: Int): RecordEntity?

    @Query("SELECT packageName, COUNT(*) AS count FROM RecordEntity GROUP BY packageName ORDER BY COUNT(*) DESC")
    suspend fun getRecordCountByPack(): List<RecordPackageCount>

    @Query("SELECT type, COUNT(*) AS count FROM RecordEntity GROUP BY type ORDER BY COUNT(*) DESC")
    suspend fun getRecordCountByType(): List<RecordTypeCount>

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta
        FROM RecordEntity
        WHERE packageName = :packageName
          AND (
            instr(record, :queryText) > 0
            OR instr(record, :fallbackText) > 0
          )
        ORDER BY time DESC
        """
    )
    fun getRecordByPackCaseSensitive(
        packageName: String,
        queryText: String,
        fallbackText: String
    ): PagingSource<Int, SmallRecordEntity>

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta
        FROM RecordEntity
        WHERE packageName = :packageName
          AND (
            instr(lower(record), :queryText) > 0
            OR instr(lower(record), :fallbackText) > 0
          )
        ORDER BY time DESC
        """
    )
    fun getRecordByPackIgnoreCase(
        packageName: String,
        queryText: String,
        fallbackText: String
    ): PagingSource<Int, SmallRecordEntity>

    @Query("SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta FROM RecordEntity WHERE packageName = :packageName ORDER BY time DESC")
    fun getRecordByPackNoPattern(packageName: String): PagingSource<Int, SmallRecordEntity>

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta
        FROM RecordEntity
        WHERE type = :type
          AND (
            instr(record, :queryText) > 0
            OR instr(record, :fallbackText) > 0
          )
        ORDER BY time DESC
        """
    )
    fun getRecordByTypeCaseSensitive(
        type: String,
        queryText: String,
        fallbackText: String
    ): PagingSource<Int, SmallRecordEntity>

    @Query(
        """
        SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta
        FROM RecordEntity
        WHERE type = :type
          AND (
            instr(lower(record), :queryText) > 0
            OR instr(lower(record), :fallbackText) > 0
          )
        ORDER BY time DESC
        """
    )
    fun getRecordByTypeIgnoreCase(
        type: String,
        queryText: String,
        fallbackText: String
    ): PagingSource<Int, SmallRecordEntity>

    @Query("SELECT id,type,subType,packageName,isRead,isMark,time,summaryTitle,summarySubtitle,summaryMeta FROM RecordEntity WHERE type = :type ORDER BY time DESC")
    fun getRecordByTypeNoPattern(type: String): PagingSource<Int, SmallRecordEntity>

    @Query("Delete FROM RecordEntity WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM RecordEntity")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM RecordEntity WHERE isRead = :isRead")
    suspend fun deleteReadRecord(isRead: Boolean)

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
