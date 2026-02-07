package me.simpleHook.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.work.impl.WorkDatabasePathHelper.getDatabasePath
import java.io.File

/**
 * 文本信息数据库工具类（直接操作SQLite数据库文件）
 * 数据库文件路径：/data/data/包名/databases/text_db.db
 */
class TextDbHelper(val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,  // 数据库文件名
    null,
    DATABASE_VERSION  // 版本号
) {
    companion object {
        private const val TAG = "TextDbHelper"
        private const val DATABASE_NAME = "record.db"  // 数据库文件名
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "text_info"  // 表名

        // 表结构：id(主键自增)、文本内容、创建时间、更新时间
        private val CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                record BLOB NOT NULL,
                create_time INTEGER NOT NULL,
                update_time INTEGER NOT NULL
            )
        """.trimIndent()
    }


    /**
     * 初始化数据库（创建表）
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_SQL)
        Log.d(TAG, "数据库表创建成功")
    }

    /**
     * 数据库版本升级
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 简单处理：删除旧表并重建（实际项目中需根据需求迁移数据）
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
        Log.d(TAG, "数据库版本升级: $oldVersion -> $newVersion")
    }

    /**
     * 插入文本信息
     * @param content 文本内容
     * @return 插入的记录ID（-1表示失败）
     */
    fun insertText(content: String, byteArray: ByteArray): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("content", content)
            val time = System.currentTimeMillis()
            put("create_time", time)
            put("update_time", time)
            put("record", byteArray)
        }
        val rowId = db.insert(TABLE_NAME, null, values)
        db.close()
        Log.d(TAG, "插入文本 ${if (rowId != -1L) "成功" else "失败"}，ID: $rowId")
        return rowId
    }


}