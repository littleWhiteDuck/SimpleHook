package me.simpleHook.platform.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.net.Uri
import me.simpleHook.core.utils.FlavorUtil
import androidx.core.net.toUri

class RecordProvider : ContentProvider() {
    private val recordsCode = 999
    private val authority = FlavorUtil.AUTHORITIES
    private var dbHelper: RecordDatabaseHelper? = null

    private val uriMatcher by lazy {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.apply {
            addURI(authority, "records", recordsCode)
        }
        matcher
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri) = when (uriMatcher.match(uri)) {
        recordsCode -> "vnd.android.cursor.dir/vnd.littleWhiteDuck.records"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?) = dbHelper?.let {
        val db = it.readableDatabase
        val uriReturn = when (uriMatcher.match(uri)) {
            recordsCode -> {
                val newLogId = db.insert("RecordEntity", null, values)
                "content://$authority/RecordEntity/$newLogId".toUri()
            }

            else -> null
        }
        uriReturn
    }

    override fun onCreate() = context?.let {
        dbHelper = RecordDatabaseHelper(it, "records.db", 1)
        true
    } ?: false

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ) = dbHelper?.let {
        val db = it.writableDatabase
        val cursor = when (uriMatcher.match(uri)) {
            recordsCode -> db.query(
                "RecordEntity", projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            else -> null
        }
        cursor
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ) = dbHelper?.let {
        val db = it.readableDatabase
        val updateRows = when (uriMatcher.match(uri)) {
            recordsCode -> db.update("RecordEntity", values, selection, selectionArgs)
            else -> 0
        }
        updateRows
    } ?: 0
}
