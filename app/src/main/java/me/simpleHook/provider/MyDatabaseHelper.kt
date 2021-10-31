package me.simpleHook.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDatabaseHelper(context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {
    private val createConfig = "create table AppConfig (" +
            "packageName text," +
            "appName text," +
            "versionName text," +
            "description text," +
            "app_config text," +
            "canUse integer," +
            "id integer primary key autoincrement)"
    private val createLog = "create table PrintLog (" +
            "packageName text," +
            "log text," +
            "id integer primary key autoincrement)"
    private val createLogConfig = "create table AssistConfig (" +
            "packageName text," +
            "config text," +
            "allSwitch integer," +
            "id integer primary key autoincrement)"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createConfig)
        db.execSQL(createLog)
        db.execSQL(createLogConfig)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {


    }
}