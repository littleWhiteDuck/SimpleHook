package me.simpleHook.util

import android.content.Context
import android.os.Build
import java.util.*

object LanguageUtils {
    fun isEnglish(context: Context): Boolean {
        val curLocale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }
        return curLocale.language == Locale.ENGLISH.language
    }
}