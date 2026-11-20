package me.simpleHook.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import me.simpleHook.data.local.db.entity.AppConfig

@Dao
interface AppConfigDao {
    @Insert
    suspend fun insertConfigs(vararg appConfig: AppConfig): List<Long>

    @Update
    suspend fun updateConfigs(vararg appConfig: AppConfig)

    @Delete
    suspend fun deleteConfigs(vararg appConfig: AppConfig)

    @Query("SELECT packageName FROM AppConfig WHERE id IN (:ids)")
    suspend fun getPackageNamesByIds(ids: List<Int>): List<String>

    @Query("SELECT * FROM AppConfig WHERE packageName = :packageName ORDER BY enable DESC, CASE WHEN id = :preferredId THEN 1 ELSE 0 END DESC, id DESC LIMIT 1")
    suspend fun getConfigForSync(packageName: String, preferredId: Int?): AppConfig?

    @Query("SELECT * FROM AppConfig ORDER BY ID DESC")
    fun queryAll(): LiveData<List<AppConfig>>

    @Query("SELECT * FROM AppConfig ORDER BY ID DESC")
    suspend fun getAll(): List<AppConfig>

    @Query("SELECT * FROM AppConfig WHERE appName LIKE:pattern or packageName LIKE:pattern ORDER BY ID DESC")
    suspend fun filterConfigs(pattern: String): List<AppConfig>

    @Query("SELECT packageName FROM AppConfig ORDER BY ID DESC")
    suspend fun getAllPackageNames(): List<String>

    @Query(
        "SELECT packageName FROM AppConfig WHERE enable = 1 " +
                "UNION " +
                "SELECT packageName FROM ExtensionConfigEntity WHERE enable = 1"
    )
    suspend fun getEnabledPackageNames(): List<String>

}
