package me.simpleHook.feature.backup.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupContentsTest {
    private fun archive(vararg entries: Pair<String, String>) = ByteArrayOutputStream().apply {
        ZipOutputStream(this).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }.toByteArray().inputStream()

    @Test fun readsAllTablesAndAcceptsDeliberatelyEmptyBackup() = runTest {
        val contents = BackupContents.read(archive(
            "custom_config.json" to "[]",
            "extension_config.json" to "[]",
            "collection_config.json" to """[{"name":"saved","config":"{}","type":"custom","id":3}]"""
        ))
        assertEquals("saved", contents.collections.single().name)
        assertTrue(contents.custom.isEmpty())
        assertTrue(BackupContents.read(archive("custom_config.json" to "[]")).custom.isEmpty())
    }

    @Test fun malformedExtensionCannotBecomePartialRestore() = runTest {
        try {
            BackupContents.read(archive("custom_config.json" to "[]", "extension_config.json" to "broken"))
            fail("Malformed extension data must reject the entire archive")
        } catch (_: kotlinx.serialization.SerializationException) { }
    }

    @Test fun unrelatedArchiveIsRejected() = runTest {
        try {
            BackupContents.read(archive("readme.txt" to "not a backup"))
            fail("An unrelated archive must not report restored")
        } catch (_: IllegalStateException) { }
    }
}
