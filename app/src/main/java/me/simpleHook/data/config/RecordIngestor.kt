package me.simpleHook.data.config

import android.content.Context
import me.simpleHook.data.local.db.entity.RecordEntity

object RecordIngestor {

    fun readFromPackages(context: Context, packageNames: Iterable<String>): List<RecordEntity> {
        val records = mutableListOf<RecordEntity>()
        packageNames.forEach { packageName ->
            val recordEntities = RecordsHelper.insertRecordsFromFile(context, packageName)
            if (recordEntities.isNotEmpty()) {
                records.addAll(recordEntities)
            }
        }
        return records
    }

    suspend fun ingestFromPackages(
        context: Context,
        packageNames: Iterable<String>,
        onBatch: suspend (List<RecordEntity>) -> Unit
    ) {
        packageNames.forEach { packageName ->
            val batch = RecordsHelper.insertRecordsFromFile(context, packageName)
            if (batch.isNotEmpty()) {
                onBatch(batch)
            }
        }
    }
}
