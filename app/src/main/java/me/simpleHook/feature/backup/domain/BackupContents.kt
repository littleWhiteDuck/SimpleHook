package me.simpleHook.feature.backup.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import me.simpleHook.data.local.db.entity.AppConfig
import me.simpleHook.data.local.db.entity.CollectionEntity
import me.simpleHook.data.local.db.entity.ExtensionConfigEntity
import java.io.InputStream
import java.util.zip.ZipInputStream

data class BackupContents(
    val custom: List<AppConfig>,
    val extension: List<ExtensionConfigEntity>,
    val collections: List<CollectionEntity>
) {
    companion object {
        /** Parse every entry before starting the database transaction. Caller owns IO context. */
        suspend fun read(input: InputStream): BackupContents {
            var custom = emptyList<AppConfig>()
            var extension = emptyList<ExtensionConfigEntity>()
            var collections = emptyList<CollectionEntity>()
            var recognized = false
            ZipInputStream(input).use { zip ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        "custom_config.json" -> { custom = Json.decodeFromString(zip.readBytes().toString(Charsets.UTF_8)); recognized = true }
                        "extension_config.json" -> { extension = Json.decodeFromString(zip.readBytes().toString(Charsets.UTF_8)); recognized = true }
                        "collection_config.json" -> { collections = Json.decodeFromString(zip.readBytes().toString(Charsets.UTF_8)); recognized = true }
                    }
                    zip.closeEntry()
                }
            }
            check(recognized) { "No supported configuration entries in backup" }
            return BackupContents(custom, extension, collections)
        }
    }
}
