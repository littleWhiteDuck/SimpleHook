package me.simpleHook.compat

import android.content.Context
import android.content.SharedPreferences
import me.simpleHook.App
import me.simpleHook.constant.Constant
import me.simpleHook.util.AppUtils

class PrefConfigHelper : ConfigSystem {
    val customPref by lazy { getHookConfigPref(App.app) }

    private val exPref by lazy {
        getHookConfigPref(
            App.app, Constant.EXTENSION_CONFIG_PREF
        )
    }

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || customPref != null
    }

    override fun saveCustomConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return customPref?.edit()?.putString(packageName, content)?.commit() ?: false
    }

    override fun deleteCustomConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return customPref?.edit()?.remove(packageName)?.commit() ?: false
    }

    override fun saveExConfig(packageName: String, content: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return exPref?.edit()?.putString(packageName, content)?.commit() ?: false
    }

    override fun deleteExConfig(packageName: String): Boolean {
        if (!AppUtils.isAppInstalled(packageName)) return true
        return exPref?.edit()?.remove(packageName)?.commit() ?: false
    }

    private fun getHookConfigPref(
        context: Context, name: String = Constant.CUSTOM_CONFIG_PREF
    ): SharedPreferences? {
        return try {
            context.getSharedPreferences(name, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            null
        }
    }
}