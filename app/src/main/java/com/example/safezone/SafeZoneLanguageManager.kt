package com.example.safezone

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Shared language manager for SafeZone.
 *
 * Supported languages:
 * English   - en
 * isiZulu   - zu
 * Setswana  - tn
 *
 * The selected language is saved locally and applied to activities
 * that use this manager.
 */
object SafeZoneLanguageManager {

    private const val PREFS_NAME = "safezone_language_preferences"
    private const val KEY_LANGUAGE = "selected_language"

    const val ENGLISH = "en"
    const val ISIZULU = "zu"
    const val SETSWANA = "tn"

    data class LanguageOption(
        val code: String,
        val displayName: String
    )

    val supportedLanguages = listOf(
        LanguageOption(ENGLISH, "English"),
        LanguageOption(ISIZULU, "isiZulu"),
        LanguageOption(SETSWANA, "Setswana")
    )

    /**
     * Returns the saved language, defaulting to English.
     */
    fun getSavedLanguage(context: Context): String {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedLanguage = preferences.getString(
            KEY_LANGUAGE,
            ENGLISH
        ) ?: ENGLISH

        return if (supportedLanguages.any {
                it.code == savedLanguage
            }
        ) {
            savedLanguage
        } else {
            ENGLISH
        }
    }

    /**
     * Saves the selected language.
     */
    fun saveLanguage(context: Context, languageCode: String) {
        require(supportedLanguages.any {
            it.code == languageCode
        }) {
            "Unsupported SafeZone language: $languageCode"
        }

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    /**
     * Wraps an Activity context with the selected locale.
     *
     * Call from attachBaseContext() before setContentView().
     */
    fun wrapContext(base: Context): Context {
        val languageCode = getSavedLanguage(base)
        val locale = Locale.forLanguageTag(languageCode)

        Locale.setDefault(locale)

        val configuration = Configuration(
            base.resources.configuration
        )

        configuration.setLocale(locale)

        return base.createConfigurationContext(configuration)
    }

    /**
     * Returns the display name for a language code.
     */
    fun getLanguageName(languageCode: String): String {
        return supportedLanguages.firstOrNull {
            it.code == languageCode
        }?.displayName ?: "English"
    }
}