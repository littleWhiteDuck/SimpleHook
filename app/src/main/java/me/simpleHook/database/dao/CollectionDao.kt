package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.database.entity.CollectionEntity


@Dao
interface CollectionDao {
    @Insert
    fun insertCollections(vararg collection: CollectionEntity)

    @Update
    fun updateCollections(vararg collection: CollectionEntity)

    @Delete
    fun deleteCollections(vararg collection: CollectionEntity)

    @Query("DELETE FROM CollectionEntity")
    fun deleteAllCollections()

    @Query("SELECT * FROM CollectionEntity ORDER BY ID DESC")
    fun queryAll(): LiveData<List<CollectionEntity>>

    @Query("SELECT * FROM CollectionEntity ORDER BY ID DESC")
    fun getAll(): List<CollectionEntity>

}