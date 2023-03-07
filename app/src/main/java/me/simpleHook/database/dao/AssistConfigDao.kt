package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.database.entity.AssistConfig

@Dao
interface AssistConfigDao {
    @Query("SELECT * FROM AssistConfig ORDER BY ID DESC")
    fun getAllConfigs(): LiveData<List<AssistConfig>>

    @Query("SELECT * FROM AssistConfig ORDER BY ID DESC")
    fun getConfigs(): List<AssistConfig>

    @Query("SELECT packageName FROM AssistConfig ORDER BY ID DESC")
    fun getAllExtensionPackageNames(): List<String>

    @Delete
    suspend fun deleteConfigs(vararg config: AssistConfig)

    @Query("DELETE FROM AssistConfig")
    fun deleteAllConfigs()

    @Query("DELETE FROM AssistConfig WHERE packageName == :packageName")
    fun deleteAssistConfigsByPackageName(packageName: String)

    @Update
    suspend fun updateConfigs(vararg config: AssistConfig)

    @Insert
    suspend fun insertConfigs(vararg config: AssistConfig)

    @Query("SELECT * FROM AssistConfig WHERE packageName LIKE :pattern or appName LIKE :pattern ORDER BY ID DESC")
    fun getFilterConfigs(pattern: String): LiveData<List<AssistConfig>>

    @Query("SELECT * FROM AssistConfig WHERE packageName = '模板配置'")
    suspend fun queryDefaultExConfig(): List<AssistConfig>


    @Query("SELECT count(*) from AssistConfig where packageName = :packageName")
    suspend fun getCountByPackageName(packageName: String): Int
}