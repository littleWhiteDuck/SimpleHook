package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.database.entity.AssistConfig

@Dao
interface AssistConfigDao {
    @Query("SELECT * FROM AssistConfig ORDER BY ID DESC")
    fun getAllConfigs(): LiveData<List<AssistConfig>>

    @Delete
    fun deleteConfigs(vararg config: AssistConfig)

    @Query("DELETE FROM AssistConfig")
    fun deleteAllConfigs()

    @Update
    fun updateConfigs(vararg config: AssistConfig)

    @Insert
    fun insertConfigs(vararg config: AssistConfig)

    @Query("SELECT * FROM AssistConfig WHERE packageName LIKE :pattern or appName LIKE :pattern ORDER BY ID DESC")
    fun getFilterConfigs(pattern: String): LiveData<List<AssistConfig>>
}