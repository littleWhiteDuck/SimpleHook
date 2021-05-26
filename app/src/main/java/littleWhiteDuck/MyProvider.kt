package littleWhiteDuck

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.net.Uri

class MyProvider:ContentProvider() {
    private val configDir = 0
    private val authority = "littleWhiteDuck"
    private var dbHelper: MyDatabaseHelper? = null

    private val uriMatcher by lazy {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.apply {
            addURI(authority, "app_configs", configDir)
        }
        matcher
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("Implement this to handle requests to delete one or more rows")
    }

    override fun getType(uri: Uri) = when (uriMatcher.match(uri)) {
        configDir -> "vnd.android.cursor.dir/vnd.littleWhiteDuck.app_configs"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?) = dbHelper?.let {
        val db = it.readableDatabase
        val uriReturn = when (uriMatcher.match(uri)) {
            configDir -> {
                val newConfigId = db.insert("Config", null, values)
                Uri.parse("content://$authority/config/$newConfigId")
            }
            else -> null
        }
        uriReturn
    }

    override fun onCreate() = context?.let {
        dbHelper = MyDatabaseHelper(it, "app_configs.db", 1)
        true
    } ?: false

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?) = dbHelper?.let {
        val db = it.writableDatabase
        val cursor = when (uriMatcher.match(uri)) {
            configDir -> db.query("AppConfigEntity", projection, selection, selectionArgs, null, null, null)
            else -> null
        }
        cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?) = dbHelper?.let {
        val db = it.readableDatabase
        val updateRows = when(uriMatcher.match(uri)){
            configDir -> db.update("Config",values,selection,selectionArgs)
            else -> 0
        }
        updateRows
    } ?: 0
}