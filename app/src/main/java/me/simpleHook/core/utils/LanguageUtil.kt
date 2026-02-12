package me.simpleHook.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import me.simpleHook.R
import java.util.*

/**
 * @author : le.hu
 * e-mail : 暂无
 * time   : 2021/11/26/16:08
 * desc   : 多语言适配方案，适配各种版本，核心未替换上下文Context中的Local
 */
@Suppress("unused")
object LanguageUtil {
    private const val TAG = "LanguageUtil"

    /**
     * 默认支持的语言，英语、法语、阿拉伯语
     */
    private val supportLanguage: HashMap<String, Locale> = object : HashMap<String, Locale>(4) {
        init {
            put(Locale.SIMPLIFIED_CHINESE.toString(), Locale.SIMPLIFIED_CHINESE)
            put(Locale.TRADITIONAL_CHINESE.toString(), Locale.TRADITIONAL_CHINESE)
            put(Locale.ENGLISH.toString(), Locale.ENGLISH)
        }
    }

    /**
     * 应用多语言切换，重写BaseActivity中的attachBaseContext即可
     * 采用本地SP存储的语言
     *
     * @param context 上下文
     * @return context
     */
    fun attachBaseContext(context: Context): Context {
        val language: String = SPUtil(context).language ?: "system"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createConfigurationContext(context, language)
        } else {
            updateConfiguration(context, language)
        }
    }

    /**
     * 应用多语言切换，重写BaseActivity中的attachBaseContext即可
     *
     * @param context  上下文
     * @param language 语言
     * @return context
     */
    fun attachBaseContext(context: Context, language: String): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createConfigurationContext(context, language)
        } else {
            updateConfiguration(context, language)
        }
    }

    /**
     * 获取Local,根据language
     *
     * @param language 语言
     * @return Locale
     */
    private fun getLanguageLocale(language: String): Locale? {
        if (supportLanguage.containsKey(language)) {
            return supportLanguage[language]
        } else {
            val systemLocal = systemLocal
            for (languageKey in supportLanguage.keys) {
                if (TextUtils.equals(
                        supportLanguage[languageKey]!!.language, systemLocal.language
                    )
                ) {
                    return systemLocal
                }
            }
        }
        return Locale.ENGLISH
    }

    /**
     * 获取当前的Local，默认英语
     *
     * @param context context
     * @return Locale
     */
    fun getCurrentLocale(context: Context): Locale {
        val language: String = SPUtil(context).language ?: "system"
        if (supportLanguage.containsKey(language)) {
            return supportLanguage[language]!!
        } else {
            val systemLocal = systemLocal
            for (languageKey in supportLanguage.keys) {
                if (TextUtils.equals(
                        supportLanguage[languageKey]!!.language, systemLocal.language
                    )
                ) {
                    return systemLocal
                }
            }
        }
        return Locale.ENGLISH
    }

    /**
     * 获取系统的Local
     *
     * @return Locale
     */
    private val systemLocal: Locale
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale.getDefault()
        }

    /**
     * Android 7.1 以下通过 updateConfiguration
     *
     * @param context  context
     * @param language 语言
     * @return Context
     */
    @Suppress("DEPRECATION")
    private fun updateConfiguration(context: Context, language: String): Context {
        val resources = context.resources
        val configuration = resources.configuration
        val locale = getLanguageLocale(language)
        Log.e(TAG, "updateLocalApiLow==== " + locale!!.language)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // apply locale
            configuration.setLocales(LocaleList(locale))
        } else {
            // updateConfiguration
            configuration.locale = locale
            val dm = resources.displayMetrics
            resources.updateConfiguration(configuration, dm)
        }
        return context
    }

    /**
     * Android 7.1以上通过createConfigurationContext
     * N增加了通过config.setLocales去修改多语言
     *
     * @param context  上下文
     * @param language 语言
     * @return context
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private fun createConfigurationContext(context: Context, language: String): Context {
        val resources = context.resources
        val configuration = resources.configuration
        val locale = getLanguageLocale(language)
        Log.d(TAG, "current Language locale = $locale")
        val localeList = LocaleList(locale)
        configuration.setLocales(localeList)
        return context.createConfigurationContext(configuration)
    }

    /**
     * 切换语言
     *
     * @param language 语言
     * @param activity 当前界面
     * @param cls      跳转的界面
     */
    fun switchLanguage(language: String, activity: Activity, cls: Class<*>?) {
        SPUtil(activity).language = language
        val intent = Intent(activity, cls)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * 切换语言，携带传递数据
     *
     * @param language 语言
     * @param activity 当前界面
     * @param cls      跳转的界面
     */
    fun switchLanguage(language: String, activity: Activity, cls: Class<*>, bundle: Bundle?) {
        SPUtil(activity).language = language
        val intent = Intent(activity, cls)
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * 获取新语言的 Context,修复了androidx.appCompact 1.2.0的问题
     *
     * @param newBase newBase
     * @return Context
     */
    fun getNewLocalContext(newBase: Context): Context {
        try {
            // 多语言适配
            val context = attachBaseContext(newBase)
            // 兼容appcompat 1.2.0后切换语言失效问题
            val configuration = context.resources.configuration
            return object : ContextThemeWrapper(context, R.style.Theme_AppCompat_Empty) {
                override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
                    overrideConfiguration.setTo(configuration)
                    super.applyOverrideConfiguration(overrideConfiguration)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newBase
    }

    /**
     * 更新Application的Resource local，应用不重启的情况才调用，因为部分会用到application中的context
     * 切记不能走新api createConfigurationContext，亲测
     * @param context context
     * @param newLanguage newLanguage
     */
    @Suppress("DEPRECATION")
    fun updateApplicationLocale(context: Context, newLanguage: String) {
        val resources = context.resources
        val configuration = resources.configuration
        val locale = getLanguageLocale(newLanguage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // apply locale
            configuration.setLocales(LocaleList(locale))
        } else {
            configuration.setLocale(locale)
        }
        val dm = resources.displayMetrics
        resources.updateConfiguration(configuration, dm)
        //context.createConfigurationContext(configuration)
    }

    fun isNotChinese(): Boolean {
        val curLocale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale.getDefault()
        }
        return curLocale.language != Locale.SIMPLIFIED_CHINESE.language || curLocale.language != Locale.TRADITIONAL_CHINESE.language
    }
}