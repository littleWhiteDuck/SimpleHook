package me.simpleHook.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDoneException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.simpleHook.data.local.db.dao.RecordDao
import me.simpleHook.data.local.db.entity.RecordEntity

class RecordRepository(context: Context, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val context = context.applicationContext

    @Volatile private var cachedDatabase: RecordDatabase? = null

    private suspend fun database(): RecordDatabase = cachedDatabase ?: withContext(io) {
        RecordDatabase.getDatabase(context).also { cachedDatabase = it }
    }
    suspend fun dao(): RecordDao = database().recordDao()

    suspend fun getRecord(id: Int): RecordEntity? {
        val database = database()
        val metadata = database.recordDao().getRecordMetaById(id) ?: return null
        val content = getRecordContent(id) ?: return null
        return metadata.copy(record = content)
    }

    private suspend fun getRecordContent(id: Int): String? {
        val database = database()
        return withContext(io) {
            // A statement avoids CursorWindow limits for large record bodies.
            database.openHelper.readableDatabase
                .compileStatement("SELECT record FROM RecordEntity WHERE id = ? LIMIT 1").use {
                    it.bindLong(1, id.toLong())
                    try { it.simpleQueryForString() } catch (_: SQLiteDoneException) { null }
                }
        }
    }

    suspend fun forEachMarked(
        filter: String,
        isType: Boolean,
        consume: suspend (String) -> Unit
    ) {
        val dao = dao()
        val upperBound = dao.getLatestRecordId()
        var beforeId = Int.MAX_VALUE
        var lastTime: String? = null
        while (true) {
            currentCoroutineContext().ensureActive()
            val ids = dao.getMarkedIds(filter, isType, lastTime, beforeId, upperBound, 100)
            if (ids.isEmpty()) return
            for (key in ids) {
                currentCoroutineContext().ensureActive()
                getRecordContent(key.id)?.let { consume(it) }
            }
            beforeId = ids.last().id
            lastTime = ids.last().time
        }
    }
}
