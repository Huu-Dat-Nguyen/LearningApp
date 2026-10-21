package com.learningapp

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The app's UI language = the user's main language. DataStore is async, so the code is mirrored
 * in plain SharedPreferences for the activity to read synchronously when it starts.
 */
object AppLocale {
    private const val FILE = "app_locale"
    private const val KEY = "main_lang"

    fun saved(context: Context): String? = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)

    fun save(context: Context, lang: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** [base] with its resources switched to the saved main language (unchanged if none saved yet). */
    fun wrap(base: Context): Context {
        val lang = saved(base) ?: return base
        // Day names and times (java.time formatting) follow the default locale.
        Locale.setDefault(Locale.forLanguageTag(lang))
        return localized(base, lang)
    }

    /** A context whose resources use [lang]; for the worker and anything outside the activity. */
    fun localized(base: Context, lang: String): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang))
        return base.createConfigurationContext(config)
    }
}
