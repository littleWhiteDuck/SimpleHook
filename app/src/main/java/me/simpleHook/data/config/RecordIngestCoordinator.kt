package me.simpleHook.data.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.simpleHook.data.local.db.RecordDatabase

object RecordIngestCoordinator {
    private const val INSERT_BATCH_SIZE = 200

    private val mutex = Mutex()

    suspend fun requestIngest(
        context: Context,
        packageNames: Iterable<String>,
        reason: RecordIngestReason,
        skipIfRunning: Boolean = false
    ): Boolean {
        val packages = packageNames.toSet()
        if (packages.isEmpty()) return false

        if (skipIfRunning) {
            if (!mutex.tryLock()) return false
            return try {
                ingestLocked(context.applicationContext, packages, reason)
                true
            } finally {
                mutex.unlock()
            }
        }

        mutex.withLock {
            ingestLocked(context.applicationContext, packages, reason)
        }
        return true
    }

    private suspend fun ingestLocked(
        context: Context,
        packageNames: Set<String>,
        @Suppress("UNUSED_PARAMETER") reason: RecordIngestReason
    ) {
        withContext(Dispatchers.IO) {
            val recordDao = RecordDatabase.getDatabase(context).recordDao()
            RecordIngestor.ingestFromPackages(context, packageNames) { records ->
                records.chunked(INSERT_BATCH_SIZE).forEach { batch ->
                    recordDao.insertRecords(*batch.toTypedArray())
                }
            }
        }
    }
}

enum class RecordIngestReason {
    SummaryRefresh,
    RecordListRefresh,
    FloatRealtime,
    Background
}
