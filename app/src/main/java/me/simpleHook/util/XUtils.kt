package me.simpleHook.util

import android.content.Context

class XUtils(context: Context, name: String) {
    val configPref = try {
        context.getSharedPreferences(
            name,
            Context.MODE_WORLD_READABLE
        )
    } catch (e: SecurityException) {
        null
    }
}