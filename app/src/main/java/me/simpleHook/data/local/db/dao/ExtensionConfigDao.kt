package me.simpleHook.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity


@Dao
interface ExtensionConfigDao {
    @Query("SELECT * FROM ExtensionConfigEntity ORDER BY ID DESC")
    fun getAllExtConfigs(): LiveData<List<ExtensionConfigEntity>>

    @Query("SELECT * FROM ExtensionConfigEntity ORDER BY ID DESC")
    fun getExtConfigs(): List<ExtensionConfigEntity>

    @Query("SELECT packageName FROM ExtensionConfigEntity ORDER BY ID DESC")
    fun getAllExtPackageNames(): List<String>

    @Delete
    suspend fun deleteExtConfigs(vararg config: ExtensionConfigEntity)

    @Query("DELETE FROM ExtensionConfigEntity")
    fun deleteAllExtConfigs()

    @Query("DELETE FROM ExtensionConfigEntity WHERE packageName == :packageName")
    fun deleteExtConfigsByPackageName(packageName: String)

    @Update
    suspend fun updateExtConfigs(vararg config: ExtensionConfigEntity)

    @Insert
    suspend fun insertExtConfigs(vararg config: ExtensionConfigEntity)

    @Query("SELECT * FROM ExtensionConfigEntity WHERE packageName LIKE :pattern or appName LIKE :pattern ORDER BY ID DESC")
    fun getFilterExtConfigs(pattern: String): LiveData<List<ExtensionConfigEntity>>

    @Query("SELECT * FROM ExtensionConfigEntity WHERE packageName = '模板配置'")
    suspend fun queryDefaultExConfigs(): List<ExtensionConfigEntity>

    @Query("SELECT count(*) from ExtensionConfigEntity where packageName = :packageName AND enable = 1")
    suspend fun getEnabledExtCountByPackageName(packageName: String): Int
}
