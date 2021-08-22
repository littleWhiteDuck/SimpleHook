package me.simpleHook.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.database.entity.AppConfig

@Dao
interface AppConfigDao {
    @Insert
    fun insertConfigs(vararg appConfig: AppConfig)
    @Update
    fun updateConfigs(vararg appConfig: AppConfig)
    @Delete
    fun deleteConfigs(vararg appConfig: AppConfig)
    @Query("DELETE FROM AppConfig")
    fun deleteAllConfigs()
    @Query("SELECT * FROM AppConfig ORDER BY ID DESC")
    fun queryAll():LiveData<List<AppConfig>>
    @Query("SELECT * FROM AppConfig WHERE packageName LIKE:pattern or appName LIKE:pattern ORDER BY ID DESC")
    fun filterConfigs(pattern:String):LiveData<List<AppConfig>>

}