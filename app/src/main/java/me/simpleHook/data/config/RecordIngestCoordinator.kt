package me.simpleHook.data.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.simpleHook.data.local.db.RecordDatabase

object RecordIngestCoordinator {
    private val mutex = Mutex()

    suspend fun requestIngest(
        context: Context,
        packageNames: Iterable<String>,
        skipIfRunning: Boolean = false
    ): RecordIngestResult {
        val packages = packageNames.toSet()
        if (packages.isEmpty()) return RecordIngestResult.Skipped

        if (skipIfRunning) {
            if (!mutex.tryLock()) return RecordIngestResult.Skipped
            return try {
                ingestLocked(context.applicationContext, packages)
            } finally {
                mutex.unlock()
            }
        }

        return mutex.withLock {
            ingestLocked(context.applicationContext, packages)
        }
    }

    private suspend fun ingestLocked(
        context: Context,
        packageNames: Set<String>
    ): RecordIngestResult = withContext(Dispatchers.IO) {
        val recordDao = RecordDatabase.getDatabase(context).recordDao()
        val failures = RecordIngestor.ingestFromPackages(context, packageNames) { records ->
            recordDao.insertRecords(*records.toTypedArray())
        }
        RecordIngestResult.Completed(failures)
    }
}

sealed interface RecordIngestResult {
    data object Skipped : RecordIngestResult
    data class Completed(val failures: Map<String, Exception>) : RecordIngestResult
}
