package me.simpleHook.util

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SPUtils(context: Context, name: String = "me.simpleHook_preferences") {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    var openStorage by SharedPreferenceDelegates.boolean()
    var openXml by SharedPreferenceDelegates.boolean()
    var updateShow by SharedPreferenceDelegates.string("1.0.1")
    var smali2Config by SharedPreferenceDelegates.boolean()
    var encryptConfigs by SharedPreferenceDelegates.boolean()
    var showByType by SharedPreferenceDelegates.boolean(true)
    var showMoreDataTip by SharedPreferenceDelegates.boolean(false)
    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    private object SharedPreferenceDelegates {

        fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SPUtils, Int> {

            override fun getValue(thisRef: SPUtils, property: KProperty<*>): Int {
                return thisRef.preferences.getInt(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtils, property: KProperty<*>, value: Int) {
                thisRef.preferences.edit().putInt(property.name, value).apply()
            }
        }

        fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SPUtils, Long> {

            override fun getValue(thisRef: SPUtils, property: KProperty<*>): Long {
                return thisRef.preferences.getLong(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtils, property: KProperty<*>, value: Long) {
                thisRef.preferences.edit().putLong(property.name, value).apply()
            }
        }

        fun boolean(defaultValue: Boolean = false) = object : ReadWriteProperty<SPUtils, Boolean> {
            override fun getValue(thisRef: SPUtils, property: KProperty<*>): Boolean {
                return thisRef.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtils, property: KProperty<*>, value: Boolean) {
                thisRef.preferences.edit().putBoolean(property.name, value).apply()
            }
        }

        fun float(defaultValue: Float = 0.0f) = object : ReadWriteProperty<SPUtils, Float> {
            override fun getValue(thisRef: SPUtils, property: KProperty<*>): Float {
                return thisRef.preferences.getFloat(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtils, property: KProperty<*>, value: Float) {
                thisRef.preferences.edit().putFloat(property.name, value).apply()
            }
        }

        fun string(defaultValue: String? = null) = object : ReadWriteProperty<SPUtils, String?> {
            override fun getValue(thisRef: SPUtils, property: KProperty<*>): String? {
                return thisRef.preferences.getString(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtils, property: KProperty<*>, value: String?) {
                thisRef.preferences.edit().putString(property.name, value).apply()
            }
        }

        fun setString(defaultValue: Set<String>? = null) =
            object : ReadWriteProperty<SPUtils, Set<String>?> {
                override fun getValue(thisRef: SPUtils, property: KProperty<*>): Set<String>? {
                    return thisRef.preferences.getStringSet(property.name, defaultValue)
                }

                override fun setValue(
                    thisRef: SPUtils,
                    property: KProperty<*>,
                    value: Set<String>?
                ) {
                    thisRef.preferences.edit().putStringSet(property.name, value).apply()
                }
            }
    }
}