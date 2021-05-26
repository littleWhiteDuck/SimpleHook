package me.simpleHook.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppConfigDao {
    @Insert
    fun insertConfigs(vararg appConfigEntity: AppConfigEntity)
    @Update
    fun updateConfigs(vararg appConfigEntity: AppConfigEntity)
    @Delete
    fun deleteConfigs(vararg appConfigEntity: AppConfigEntity)
    @Query("DELETE FROM AppConfigEntity")
    fun deleteAllConfigs()
    @Query("SELECT * FROM AppConfigEntity ORDER BY ID DESC")
    fun queryAll():LiveData<List<AppConfigEntity>>

}