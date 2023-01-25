package me.simpleHook.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.net.Uri
import me.simpleHook.BuildConfig
import me.simpleHook.util.FlavorUtils

class MyProvider : ContentProvider() {
    private val configDir = 0
    private val printLogDir = 1
    private val assistConfig = 2
    private val authority =
        if (FlavorUtils.isLiteVersion) "me.simplehook.lite.provider" else "me.simplehook.provider"
    private var dbHelper: MyDatabaseHelper? = null

    private val uriMatcher by lazy {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.apply {
            addURI(authority, "app_configs", configDir)
            addURI(authority, "print_logs", printLogDir)
            addURI(authority, "assist_configs", assistConfig)
        }
        matcher
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("Implement this to handle requests to delete one or more rows")
    }

    override fun getType(uri: Uri) = when (uriMatcher.match(uri)) {
        configDir -> "vnd.android.cursor.item/vnd.littleWhiteDuck.app_configs"
        printLogDir -> "vnd.android.cursor.dir/vnd.littleWhiteDuck.print_logs"
        assistConfig -> "vnd.android.cursor.dir/vnd.littleWhiteDuck.assist_configs"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?) = dbHelper?.let {
        val db = it.readableDatabase
        val uriReturn = when (uriMatcher.match(uri)) {
            configDir -> {
                val newConfigId = db.insert("AppConfig", null, values)
                Uri.parse("content://$authority/AppConfig/$newConfigId")
            }
            printLogDir -> {
                val newLogId = db.insert("PrintLog", null, values)
                Uri.parse("content://$authority/PrintLog/$newLogId")
            }
            else -> null
        }
        uriReturn
    }

    override fun onCreate() = context?.let {
        dbHelper = MyDatabaseHelper(it, "app_configs.db", 5)
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
            configDir -> db.query(
                "AppConfig",
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            printLogDir -> db.query(
                "PrintLog",
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            assistConfig -> db.query(
                "AssistConfig",
                projection,
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
            configDir -> db.update("AppConfig", values, selection, selectionArgs)
            else -> 0
        }
        updateRows
    } ?: 0
}