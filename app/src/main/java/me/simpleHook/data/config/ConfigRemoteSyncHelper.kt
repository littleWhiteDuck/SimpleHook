package me.simpleHook.data.config

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.simpleHook.platform.lsposed.LSPosedHelper

enum class ConfigBackendStatus { Saved, Failed, NotApplicable }

data class ConfigSyncResult(val local: ConfigBackendStatus, val remote: ConfigBackendStatus) {
    // Preserve fallback support, while exposing a partial result instead of claiming full success.
    val hasSavedCopy: Boolean get() = local == ConfigBackendStatus.Saved || remote == ConfigBackendStatus.Saved
    val isComplete: Boolean get() = hasSavedCopy && local != ConfigBackendStatus.Failed && remote != ConfigBackendStatus.Failed
}

object ConfigRemoteSyncHelper {
    suspend fun saveCustomConfig(configSystem: ConfigSystem, packageName: String, content: String) =
        sync({ configSystem.saveCustomConfig(packageName, content) }) {
            LSPosedHelper.writeCustomConfig(packageName, content)
        }

    suspend fun saveExtensionConfig(configSystem: ConfigSystem, packageName: String, content: String) =
        sync({ configSystem.saveExConfig(packageName, content) }) {
            LSPosedHelper.writeExtensionConfig(packageName, content)
        }

    suspend fun deleteCustomConfig(configSystem: ConfigSystem, packageName: String) =
        sync({ configSystem.deleteCustomConfig(packageName) }) { LSPosedHelper.deleteCustomConfig(packageName) }

    suspend fun deleteExtensionConfig(configSystem: ConfigSystem, packageName: String) =
        sync({ configSystem.deleteExConfig(packageName) }) { LSPosedHelper.deleteExtensionConfig(packageName) }

    private suspend fun sync(local: () -> Boolean, remote: suspend () -> Boolean): ConfigSyncResult =
        withContext(Dispatchers.IO) {
            val localResult = status { local() }
            val remoteResult = if (LSPosedHelper.isActivated()) status { remote() }
                else ConfigBackendStatus.NotApplicable
            ConfigSyncResult(localResult, remoteResult)
        }

    private suspend fun status(write: suspend () -> Boolean): ConfigBackendStatus = try {
        if (write()) ConfigBackendStatus.Saved else ConfigBackendStatus.Failed
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ConfigBackendStatus.Failed
    }
}
