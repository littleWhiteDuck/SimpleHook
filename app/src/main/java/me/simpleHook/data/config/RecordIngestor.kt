package me.simpleHook.data.config

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import me.simpleHook.data.local.db.entity.RecordEntity

object RecordIngestor {
    internal suspend fun ingestFromPackages(
        context: Context,
        packageNames: Iterable<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): Map<String, Exception> {
        return ingestPackages(packageNames, { name, commit ->
            RecordsHelper.ingestRecordsFromFile(context, name, commit)
        }, onBatch)
    }

    internal suspend fun ingestPackages(
        packageNames: Iterable<String>,
        readPackage: suspend (String, suspend (List<RecordEntity>) -> Unit) -> Unit,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ): Map<String, Exception> {
        val failures = linkedMapOf<String, Exception>()
        val coroutineContext = currentCoroutineContext()
        packageNames.forEach { packageName ->
            coroutineContext.ensureActive()
            try {
                // A segment may be acknowledged only after its final batch is committed.
                readPackage(packageName, onBatch)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failures[packageName] = error
            }
        }
        return failures
    }
}
