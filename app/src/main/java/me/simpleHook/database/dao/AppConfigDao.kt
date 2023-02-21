package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.database.entity.AppConfig

@Dao
interface AppConfigDao {
    @Insert
    suspend fun insertConfigs(vararg appConfig: AppConfig)

    @Update
    suspend fun updateConfigs(vararg appConfig: AppConfig)

    @Delete
    suspend fun deleteConfigs(vararg appConfig: AppConfig)

    @Query("DELETE FROM AppConfig")
    fun deleteAllConfigs()

    @Query("SELECT * FROM AppConfig ORDER BY ID DESC")
    fun queryAll(): LiveData<List<AppConfig>>

    @Query("SELECT * FROM AppConfig ORDER BY ID DESC")
    fun getAll(): List<AppConfig>

    @Query("SELECT * FROM AppConfig WHERE appName LIKE:pattern or packageName LIKE:pattern ORDER BY ID DESC")
    suspend fun filterConfigs(pattern: String): List<AppConfig>

    @Query("SELECT packageName FROM AppConfig ORDER BY ID DESC")
    fun getAllPackageNames(): List<String>

    @Query("SELECT count(*) from AppConfig where packageName = :packageName")
    suspend fun getCountByPackageName(packageName: String): Int

}