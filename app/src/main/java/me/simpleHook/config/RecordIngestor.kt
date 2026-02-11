package me.simpleHook.config

import android.content.Context
import me.simpleHook.database.entity.RecordEntity

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
}

