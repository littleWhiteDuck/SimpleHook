package me.simpleHook.data.config

import android.content.Context
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
        val mergedBatch = ArrayList<RecordEntity>(MERGED_BATCH_SIZE)
        packageNames.forEach { packageName ->
            RecordsHelper.ingestRecordsFromFile(context, packageName) { batch ->
                mergedBatch.addAll(batch)
                if (mergedBatch.size >= MERGED_BATCH_SIZE) {
                    onBatch(ArrayList(mergedBatch))
                    mergedBatch.clear()
                }
            }
        }
        if (mergedBatch.isNotEmpty()) {
            onBatch(ArrayList(mergedBatch))
        }
    }

    suspend fun ingestFromPackages(
        context: Context,
        packageNames: Iterable<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        val mergedBatch = ArrayList<RecordEntity>(MERGED_BATCH_SIZE)
        packageNames.forEach { packageName ->
            RecordsHelper.ingestRecordsFromFile(context, packageName) { batch ->
                mergedBatch.addAll(batch)
                if (mergedBatch.size >= MERGED_BATCH_SIZE) {
                    onBatch(ArrayList(mergedBatch))
                    mergedBatch.clear()
                }
            }
        }
        if (mergedBatch.isNotEmpty()) {
            onBatch(ArrayList(mergedBatch))
        }
    }
}
