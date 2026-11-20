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
    suspend fun getExtConfigs(): List<ExtensionConfigEntity>

    @Query("SELECT packageName FROM ExtensionConfigEntity ORDER BY ID DESC")
    suspend fun getAllExtPackageNames(): List<String>

    @Delete
    suspend fun deleteExtConfigs(vararg config: ExtensionConfigEntity)

    @Query("SELECT packageName FROM ExtensionConfigEntity WHERE id IN (:ids)")
    suspend fun getPackageNamesByIds(ids: List<Int>): List<String>

    @Query("SELECT * FROM ExtensionConfigEntity WHERE packageName = :packageName ORDER BY enable DESC, CASE WHEN id = :preferredId THEN 1 ELSE 0 END DESC, id DESC LIMIT 1")
    suspend fun getConfigForSync(packageName: String, preferredId: Int?): ExtensionConfigEntity?

    @Query("DELETE FROM ExtensionConfigEntity WHERE packageName == :packageName")
    suspend fun deleteExtConfigsByPackageName(packageName: String)

    @Update
    suspend fun updateExtConfigs(vararg config: ExtensionConfigEntity)

    @Insert
    suspend fun insertExtConfigs(vararg config: ExtensionConfigEntity): List<Long>

    @Query("SELECT * FROM ExtensionConfigEntity WHERE packageName LIKE :pattern or appName LIKE :pattern ORDER BY ID DESC")
    fun getFilterExtConfigs(pattern: String): LiveData<List<ExtensionConfigEntity>>

    @Query("SELECT * FROM ExtensionConfigEntity WHERE packageName = '模板配置'")
    suspend fun queryDefaultExConfigs(): List<ExtensionConfigEntity>

}
