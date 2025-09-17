package me.simpleHook.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import me.simpleHook.App
import me.simpleHook.GlobalValue
import me.simpleHook.constant.Constant
import me.simpleHook.util.AppUtils

class PrefConfigHelper : ConfigSystem {
    val customPref by lazy { getHookConfigPref(App) }

    private val exPref by lazy {
        getHookConfigPref(App, Constant.EXTENSION_CONFIG_PREF)
    }

    override fun isEnableSave(packageName: String): Boolean {
        return !AppUtils.isAppInstalled(packageName) || !GlobalValue.sp.checkPermission || customPref != null
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

    @SuppressLint("WorldReadableFiles")
    private fun getHookConfigPref(
        context: Context, name: String = Constant.CUSTOM_CONFIG_PREF
    ): SharedPreferences? {
        return try {
            @Suppress("DEPRECATION") context.getSharedPreferences(name, Context.MODE_WORLD_READABLE)
        } catch (_: SecurityException) {
            null
        }
    }
}