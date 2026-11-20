package me.simpleHook.data.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.simpleHook.core.constant.Constant
import me.simpleHook.core.utils.FlavorUtil
import me.simpleHook.data.local.db.AppDatabase
import me.simpleHook.platform.lsposed.LSPosedHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ConfigSyncTargets(
    // A null ID chooses a remaining enabled row after deletion/rename.
    val custom: Map<String, Int?> = emptyMap(),
    val extension: Map<String, Int?> = emptyMap()
)

/** Shared by edits, restore and migration; no file or Binder work runs in a DB transaction. */
object ConfigWriteCoordinator {
    private val mutex = Mutex()
    private val pendingCustom = mutableMapOf<String, Int?>()
    private val pendingExtension = mutableMapOf<String, Int?>()

    suspend fun <T> write(block: suspend () -> T): T = mutex.withLock { block() }

    /** Must be called inside write, after the reordered rows have been committed. */
    fun remapPendingCustomIdsLocked(idMapping: Map<Int, Int>) {
        for (entry in pendingCustom.entries) {
            val preferredId = entry.value ?: continue
            val newId = idMapping[preferredId] ?: continue
            entry.setValue(newId)
        }
    }

    /** Must be called inside write. Pending names include deleted/renamed packages for retry. */
    suspend fun syncLocked(context: Context, targets: ConfigSyncTargets = ConfigSyncTargets()) {
        pendingCustom += targets.custom.filterKeys { it != Constant.MODEL_EXTENSION_CONFIG }
        pendingExtension += targets.extension.filterKeys { it != Constant.MODEL_EXTENSION_CONFIG }
        if (pendingCustom.isEmpty() && pendingExtension.isEmpty()) return
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val system = ConfigSystemUtil.getConfigSystem()
            if (FlavorUtil.rootVersion) {
                val packages = pendingCustom.keys + pendingExtension.keys
                val enabled = db.getAppConfigDao().getEnabledPackageNames().toSet()
                // Scopes reflect committed DB state; requests are not authorization grants.
                LSPosedHelper.requestScopes(packages.filter { it in enabled })
                LSPosedHelper.removeScopes(packages.filter { it !in enabled })
            }
            val failures = mutableListOf<String>()
            val customIterator = pendingCustom.iterator()
            while (customIterator.hasNext()) {
                val (name, preferredId) = customIterator.next()
                val config = db.getAppConfigDao().getConfigForSync(name, preferredId)
                val result = if (config == null) ConfigRemoteSyncHelper.deleteCustomConfig(system, name)
                    else ConfigRemoteSyncHelper.saveCustomConfig(system, name, Json.encodeToString(config))
                if (result.isComplete) customIterator.remove() else failures += "$name (custom: $result)"
            }
            val extensionIterator = pendingExtension.iterator()
            while (extensionIterator.hasNext()) {
                val (name, preferredId) = extensionIterator.next()
                val config = db.getExtensionConfigDao().getConfigForSync(name, preferredId)
                val result = if (config == null) ConfigRemoteSyncHelper.deleteExtensionConfig(system, name)
                    else ConfigRemoteSyncHelper.saveExtensionConfig(system, name, config.config)
                if (result.isComplete) extensionIterator.remove() else failures += "$name (extension: $result)"
            }
            check(failures.isEmpty()) { "External sync failed: ${failures.joinToString()}" }
        }
    }
}
