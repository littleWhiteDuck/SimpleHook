package me.simpleHook.platform.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RecordDatabaseHelper(context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {

    companion object {
        // Keep in sync with Room database version for records.db.
        const val DB_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase?) = Unit

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
}
