package littleWhiteDuck

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDatabaseHelper(context: Context,name:String,version:Int):SQLiteOpenHelper(context,name,null,version) {
    private val createConfig = "create table AppConfigEntity (" +
            "packageName text," +
            "appName text," +
            "versionName text" +
            "description text" +
            "app_config text" +
            "canUse integer" +
            "id integer primary key autoincrement)"
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createConfig)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }
}