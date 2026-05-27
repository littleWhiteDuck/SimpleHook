package me.simpleHook.data.config

import android.content.Context
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import me.simpleHook.data.local.db.entity.RecordEntity

object RecordIngestor {
    private const val MERGED_BATCH_SIZE = 200

    fun readFromPackages(context: Context, packageNames: Iterable<String>): List<RecordEntity> {
        val records = mutableListOf<RecordEntity>()
        kotlinx.coroutines.runBlocking {
            ingestRecordsFromPackages(context, packageNames) { batch ->
                records.addAll(batch)
            }
        }
        return records
    }

    suspend fun ingestRecordsFromPackages(
        context: Context,
        packageNames: Iterable<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        ingestPackages(context, packageNames, onBatch)
    }

    suspend fun ingestFromPackages(
        context: Context,
        packageNames: Iterable<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        ingestPackages(context, packageNames, onBatch)
    }

    private suspend fun ingestPackages(
        context: Context,
        packageNames: Iterable<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val mergedBatch = ArrayList<RecordEntity>(MERGED_BATCH_SIZE)
        val coroutineContext = currentCoroutineContext()
        packageNames.forEach { packageName ->
            coroutineContext.ensureActive()
            RecordsHelper.ingestRecordsFromFile(context, packageName) { batch ->
                mergedBatch.addAll(batch)
                if (mergedBatch.size >= MERGED_BATCH_SIZE) {
                    onBatch(ArrayList(mergedBatch))
                    mergedBatch.clear()
                }
            }
        }
        coroutineContext.ensureActive()
        if (mergedBatch.isNotEmpty()) {
            onBatch(ArrayList(mergedBatch))
        }
    }
}
