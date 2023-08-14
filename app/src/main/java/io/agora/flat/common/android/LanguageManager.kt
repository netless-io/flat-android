package io.agora.flat.common.android

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.core.content.edit
import io.agora.flat.R
import java.util.*

object LanguageManager {
    private const val KEY_LANGUAGE = "key_language"

    private var store: SharedPreferences? = null

    /**
     * language: Country code
     */
    enum class Item(val language: String, val locale: Locale?, @StringRes val display: Int) {
        Default("", null, R.string.language_follow_system),
        English("en", Locale.ENGLISH, R.string.language_english),
        Chinese("zh", Locale.CHINA, R.string.language_chinese),
        ;

        companion object {
            fun of(language: String): Item {
                return values().find { it.language == language } ?: Default
            }
        }
    }

    fun init(context: Context) {
        store = context.getSharedPreferences("flat_language", Context.MODE_PRIVATE)
    }

    fun current(): String {
        return store?.getString(KEY_LANGUAGE, "") ?: ""
    }

    fun update(language: String) {
        store?.edit(commit = true) {
            putString(KEY_LANGUAGE, language)
        }
    }

    fun currentLocale(): Locale {
        return getLocale(current())
    }

    fun onAttach(context: Context): Context {
        val language = current()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configLanguage(context, language)
        } else {
            configLanguageLegacy(context, language)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun configLanguage(context: Context, language: String): Context {
        val resources = context.resources
        val configuration = resources.configuration
        val locale = getLocale(language)
        configuration.setLocale(locale)

        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun configLanguageLegacy(context: Context, language: String): Context {
        val resources = context.resources
        val configuration = resources.configuration
        val locale = getLocale(language)
        configuration.setLocale(locale)

        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    private fun getLocale(language: String): Locale {
        return Item.of(language).locale ?: getSystemDefaultLocale()
    }

    private fun getSystemDefaultLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            Locale.getDefault()
        }
    }
}