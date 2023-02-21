package me.simpleHook.lsposed

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import me.simpleHook.App
import me.simpleHook.BuildConfig
import me.simpleHook.util.AppUtils

object LSPosedHelper {
    private const val dbPath = "/data/adb/lspd/config/modules_config.db"
    private val cacheFile by lazy {
        App.getExternalFilesDir(null)!!.resolve("lsposed").also {
            it.mkdir()
        }
    }

    fun addScope(packageNames: Array<String>) {
        changeScope(packageNames, false)
    }

    fun removeScope(packageNames: Array<String>) {
        changeScope(packageNames, true)
    }

    @Synchronized
    private fun changeScope(
        packageNames: Array<String>, isRemove: Boolean
    ) {
        runCatching {
            val dbFile = cacheFile.resolve("modules_config.db")
            val file = SuFile.open(dbPath)
            SuFileInputStream.open(file).use {
                dbFile.outputStream().use { outPut ->
                    it.copyTo(outPut)
                }
            }
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            val mid = getModuleId(db, BuildConfig.APPLICATION_ID)
            if (mid == -1) return
            packageNames.forEach {
                if (AppUtils.isAppInstalled(it)) {
                    runCatching {
                        if (AppUtils.isAppInstalled(it)) {
                            if (isRemove) {
                                db.delete("scope",
                                    "mid= ? and app_pkg_name = ?",
                                    arrayOf(mid.toString(), it))
                            } else {
                                val contentValues = contentValuesOf("mid" to mid,
                                    "app_pkg_name" to it,
                                    "user_id" to 0)
                                db.insertWithOnConflict("scope",
                                    null,
                                    contentValues,
                                    SQLiteDatabase.CONFLICT_REPLACE)
                            }
                        }
                    }
                }
                dbFile.inputStream().use {
                    SuFileOutputStream.open(file).use { outPut ->
                        it.copyTo(outPut)
                    }
                }
            }
        }
    }

    private fun getModuleId(db: SQLiteDatabase, packageName: String): Int {
        val cursor = db.query("modules",
            arrayOf("mid"),
            "module_pkg_name=?",
            arrayOf(packageName),
            null,
            null,
            null) ?: return -1
        if (cursor.count != 1) return -1
        cursor.moveToFirst()
        return cursor.getInt(cursor.getColumnIndexOrThrow("mid"))
    }
}