package me.simpleHook.data.config

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.data.record.RecordType
import org.junit.Assert.*
import org.junit.Test

class RecordIngestorTest {
    private fun record(id: Int) = RecordEntity(
        type = RecordType.entries.first(), subType = "", record = "{}", packageName = "app",
        time = "", sourceKey = "segment#$id"
    )

    @Test fun segmentTailIsCommittedBeforeAcknowledgment() = runTest {
        for (count in listOf(1, 199, 200, 201, 401)) {
            val committed = mutableListOf<RecordEntity>()
            var acknowledged = false
            val failures = RecordIngestor.ingestPackages(
                listOf("app"),
                readPackage = { _, commit ->
                    (1..count).map(::record).chunked(200).forEach { commit(it) }
                    assertEquals(count, committed.size)
                    acknowledged = true
                },
                onBatch = { committed.addAll(it) }
            )
            assertTrue(acknowledged)
            assertTrue(failures.isEmpty())
        }
    }

    @Test fun failedSecondBatchRetainsSourceAndReportsPackage() = runTest {
        var acknowledged = false
        var committed = 0
        val failures = RecordIngestor.ingestPackages(
            listOf("app"),
            readPackage = { _, commit ->
                (1..201).map(::record).chunked(200).forEach { commit(it) }
                acknowledged = true
            },
            onBatch = {
                if (committed == 200) error("disk full")
                committed += it.size
            }
        )
        assertEquals(200, committed)
        assertFalse(acknowledged)
        assertEquals(setOf("app"), failures.keys)
    }

    @Test fun cancellationAfterCommitDoesNotAcknowledgeOrContinueToNextPackage() = runTest {
        var acknowledged = false
        val visited = mutableListOf<String>()
        val committed = mutableListOf<RecordEntity>()
        try {
            RecordIngestor.ingestPackages(
                listOf("app", "next"),
                readPackage = { name, commit ->
                    visited += name
                    commit(listOf(record(1)))
                    acknowledged = true
                },
                onBatch = {
                    committed.addAll(it)
                    throw CancellationException("page closed")
                }
            )
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, committed.size)
            assertEquals(listOf("app"), visited)
            assertFalse(acknowledged)
        }
    }
}
