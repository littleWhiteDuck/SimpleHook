package me.simpleHook.lsposed

import android.database.sqlite.SQLiteDatabase
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import me.simpleHook.App
import me.simpleHook.BuildConfig
import me.simpleHook.util.AppUtils

object LSPosedHelper {
    private val cacheFile by lazy {
        App.getExternalFilesDir(null)!!.resolve("lsposed").also {
            it.mkdir()
        }
    }

    fun addScope(packageNames: Array<String>) {
        changeScope(packageNames,
            "insert into scope (mid, app_pkg_name, user_id) values ((select mid from modules where module_pkg_name = ?), ?, 0)")
    }

    fun removeScope(packageNames: Array<String>) {
        changeScope(packageNames,
            "delete from scope where mid = (select mid from modules where module_pkg_name = ?) and app_pkg_name = ?")
    }

    private fun changeScope(packageNames: Array<String>, sql: String) {
        runCatching {
            val dbFile = cacheFile.resolve("modules_config.db")
            val file = SuFile.open("/data/adb/lspd/config/modules_config.db")
            SuFileInputStream.open(file).use {
                dbFile.outputStream().use { outPut ->
                    it.copyTo(outPut)
                }
            }
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            packageNames.forEach {
                if (AppUtils.isAppInstalled(it)) {
                    runCatching {
                        if (AppUtils.isAppInstalled(it)) {
                            db.execSQL(sql, arrayOf(BuildConfig.APPLICATION_ID, it))
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