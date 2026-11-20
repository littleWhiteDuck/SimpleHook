package me.simpleHook.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.data.local.db.entity.CollectionEntity


@Dao
interface CollectionDao {
    @Insert
    suspend fun insertCollections(vararg collection: CollectionEntity)

    @Update
    suspend fun updateCollections(vararg collection: CollectionEntity)

    @Delete
    suspend fun deleteCollections(vararg collection: CollectionEntity)

    @Query("DELETE FROM CollectionEntity")
    suspend fun deleteAllCollections()

    @Query("SELECT * FROM CollectionEntity ORDER BY ID DESC")
    fun queryAll(): LiveData<List<CollectionEntity>>

    @Query("SELECT * FROM CollectionEntity ORDER BY ID DESC")
    suspend fun getAll(): List<CollectionEntity>

}
