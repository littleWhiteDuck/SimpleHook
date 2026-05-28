package me.simpleHook.core.utils

import android.content.Context
import android.content.SharedPreferences
import me.simpleHook.BuildConfig
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import androidx.core.content.edit

@Suppress("PropertyName")
open class SPUtil(context: Context, name: String = BuildConfig.APPLICATION_ID + "_preferences") {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    var smali2Config by SharedPreferenceDelegates.boolean(true)
    var language by SharedPreferenceDelegates.string("system")
    var showByType by SharedPreferenceDelegates.boolean(true)
    var showMoreDataTip by SharedPreferenceDelegates.boolean(false)
    var appListSortSelected by SharedPreferenceDelegates.int(0)
    var appListReverse by SharedPreferenceDelegates.boolean(false)
    var readIntroduction by SharedPreferenceDelegates.boolean(true)
    var startFloat by SharedPreferenceDelegates.boolean(false)
    var bottomConfigDialog by SharedPreferenceDelegates.boolean(true)
    var themeMode by SharedPreferenceDelegates.string("system")
    var checkPermission by SharedPreferenceDelegates.boolean(true)
    var wordWrap by SharedPreferenceDelegates.boolean(true)
    var record_line_number by SharedPreferenceDelegates.boolean(true)
    var record_magnifier_enable by SharedPreferenceDelegates.boolean(true)
    var backup_scope by SharedPreferenceDelegates.string("BACKUP_SCOPE_ALL")
    var backup_cloud_auto by SharedPreferenceDelegates.boolean(false)
    var backup_local_auto by SharedPreferenceDelegates.boolean(false)
    var backup_path by SharedPreferenceDelegates.string("")
    var backup_cover by SharedPreferenceDelegates.string("BACKUP_OVER_MINUTE")
    var web_dav_host by SharedPreferenceDelegates.string("")
    var web_dav_account by SharedPreferenceDelegates.string("")
    var web_dav_pw by SharedPreferenceDelegates.string("")
    var auto_x_param by SharedPreferenceDelegates.boolean(true)
    var config_item_show_desc by SharedPreferenceDelegates.boolean(true)
    var configItemDescDefaultMigrated by SharedPreferenceDelegates.boolean(false)
    var enableSystemAccent by SharedPreferenceDelegates.boolean(true)
    var mediaPathConfigMigrated by SharedPreferenceDelegates.boolean(false)

    var workMode by SharedPreferenceDelegates.string("Root")

    var recordCardStyle by SharedPreferenceDelegates.boolean(false)


    fun remove(key: String) {
        preferences.edit { remove(key) }
    }

    fun ensureConfigItemDescVisibleByDefault() {
        if (configItemDescDefaultMigrated) return
        config_item_show_desc = true
        configItemDescDefaultMigrated = true
    }

    @Suppress("unused")
    private object SharedPreferenceDelegates {

        fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SPUtil, Int> {

            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Int {
                return thisRef.preferences.getInt(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Int) {
                thisRef.preferences.edit { putInt(property.name, value) }
            }
        }

        fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SPUtil, Long> {

            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Long {
                return thisRef.preferences.getLong(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Long) {
                thisRef.preferences.edit { putLong(property.name, value) }
            }
        }

        fun boolean(defaultValue: Boolean = false) = object : ReadWriteProperty<SPUtil, Boolean> {
            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Boolean {
                return thisRef.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Boolean) {
                thisRef.preferences.edit { putBoolean(property.name, value) }
            }
        }

        fun float(defaultValue: Float = 0.0f) = object : ReadWriteProperty<SPUtil, Float> {
            override fun getValue(thisRef: SPUtil, property: KProperty<*>): Float {
                return thisRef.preferences.getFloat(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: Float) {
                thisRef.preferences.edit { putFloat(property.name, value) }
            }
        }

        fun string(defaultValue: String? = null) = object : ReadWriteProperty<SPUtil, String?> {
            override fun getValue(thisRef: SPUtil, property: KProperty<*>): String? {
                return thisRef.preferences.getString(property.name, defaultValue)
            }

            override fun setValue(thisRef: SPUtil, property: KProperty<*>, value: String?) {
                thisRef.preferences.edit { putString(property.name, value) }
            }
        }

        fun setString(defaultValue: Set<String>? = null) =
            object : ReadWriteProperty<SPUtil, Set<String>?> {
                override fun getValue(thisRef: SPUtil, property: KProperty<*>): Set<String>? {
                    return thisRef.preferences.getStringSet(property.name, defaultValue)
                }

                override fun setValue(
                    thisRef: SPUtil, property: KProperty<*>, value: Set<String>?
                ) {
                    thisRef.preferences.edit { putStringSet(property.name, value) }
                }
            }
    }
}
