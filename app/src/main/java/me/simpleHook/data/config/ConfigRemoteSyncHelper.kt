package me.simpleHook.data.config

import me.simpleHook.platform.lsposed.LSPosedHelper

object ConfigRemoteSyncHelper {

    suspend fun saveCustomConfig(
        configSystem: ConfigSystem,
        packageName: String,
        content: String
    ): Boolean {
        val localSaved = configSystem.saveCustomConfig(packageName, content)
        val remoteSaved = LSPosedHelper.writeCustomConfig(packageName, content)
        return localSaved || remoteSaved
    }

    suspend fun saveExtensionConfig(
        configSystem: ConfigSystem,
        packageName: String,
        content: String
    ): Boolean {
        val localSaved = configSystem.saveExConfig(packageName, content)
        val remoteSaved = LSPosedHelper.writeExtensionConfig(packageName, content)
        return localSaved || remoteSaved
    }

    suspend fun deleteCustomConfig(configSystem: ConfigSystem, packageName: String): Boolean {
        val localDeleted = configSystem.deleteCustomConfig(packageName)
        val remoteDeleted = LSPosedHelper.deleteCustomConfig(packageName)
        return localDeleted || remoteDeleted
    }

    suspend fun deleteExtensionConfig(configSystem: ConfigSystem, packageName: String): Boolean {
        val localDeleted = configSystem.deleteExConfig(packageName)
        val remoteDeleted = LSPosedHelper.deleteExtensionConfig(packageName)
        return localDeleted || remoteDeleted
    }
}
