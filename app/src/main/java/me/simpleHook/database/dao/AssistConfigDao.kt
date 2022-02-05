package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.database.entity.AssistConfig

@Dao
interface AssistConfigDao {
    @Query("SELECT * FROM AssistConfig ORDER BY ID DESC")
    fun getAllConfigs(): LiveData<List<AssistConfig>>

    @Delete
    suspend fun deleteConfigs(vararg config: AssistConfig)

    @Query("DELETE FROM AssistConfig")
    fun deleteAllConfigs()

    @Update
    suspend fun updateConfigs(vararg config: AssistConfig)

    @Insert
    suspend fun insertConfigs(vararg config: AssistConfig)

    @Query("SELECT * FROM AssistConfig WHERE packageName LIKE :pattern or appName LIKE :pattern ORDER BY ID DESC")
    fun getFilterConfigs(pattern: String): LiveData<List<AssistConfig>>

    @Query("SELECT * FROM AssistConfig WHERE packageName = :packageName and appName = :appName")
    suspend fun queryDefaultExConfig(
        packageName: String = "默认配置",
        appName: String = "默认配置"
    ): List<AssistConfig>
}