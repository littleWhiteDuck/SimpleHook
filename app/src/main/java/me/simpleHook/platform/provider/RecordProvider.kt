package me.simpleHook.platform.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.content.UriMatcher
import android.net.Uri
import me.simpleHook.core.utils.FlavorUtil
import androidx.core.net.toUri
import androidx.sqlite.db.SimpleSQLiteQuery
import me.simpleHook.data.local.db.RecordDatabase

class RecordProvider : ContentProvider() {
    private val recordsCode = 999
    private val authority = FlavorUtil.AUTHORITIES

    private val uriMatcher by lazy {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.apply {
            addURI(authority, "records", recordsCode)
        }
        matcher
    }

    override fun getType(uri: Uri) = when (uriMatcher.match(uri)) {
        recordsCode -> "vnd.android.cursor.dir/vnd.littleWhiteDuck.records"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?) = writableDb()?.let { db ->
        val uriReturn = when (uriMatcher.match(uri)) {
            recordsCode -> {
                val newLogId = db.insert(
                    "RecordEntity",
                    SQLiteDatabase.CONFLICT_NONE,
                    values ?: ContentValues()
                )
                "content://$authority/RecordEntity/$newLogId".toUri()
            }

            else -> null
        }
        uriReturn
    }

    override fun onCreate() = context != null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val db = readableDb() ?: return null
        return when (uriMatcher.match(uri)) {
            recordsCode -> {
                val columns = projection?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "*"
                val sql = buildString {
                    append("SELECT ")
                    append(columns)
                    append(" FROM RecordEntity")
                    if (!selection.isNullOrBlank()) {
                        append(" WHERE ")
                        append(selection)
                    }
                    if (!sortOrder.isNullOrBlank()) {
                        append(" ORDER BY ")
                        append(sortOrder)
                    }
                }
                db.query(SimpleSQLiteQuery(sql, selectionArgs ?: emptyArray()))
            }
            else -> null
        }
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ) = writableDb()?.let { db ->
        val updateRows = when (uriMatcher.match(uri)) {
            recordsCode -> {
                val safeValues = values ?: return@let 0
                db.update(
                    "RecordEntity",
                    SQLiteDatabase.CONFLICT_NONE,
                    safeValues,
                    selection,
                    selectionArgs
                )
            }
            else -> 0
        }
        updateRows
    } ?: 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = writableDb() ?: return 0
        return when (uriMatcher.match(uri)) {
            recordsCode -> db.delete("RecordEntity", selection, selectionArgs)
            else -> 0
        }
    }

    private fun readableDb() = context?.let {
        RecordDatabase.getDatabase(it).openHelper.readableDatabase
    }

    private fun writableDb() = context?.let {
        RecordDatabase.getDatabase(it).openHelper.writableDatabase
    }
}
