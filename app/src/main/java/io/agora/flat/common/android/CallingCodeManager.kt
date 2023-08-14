package io.agora.flat.common.android

import android.app.Application
import android.os.Build
import android.os.LocaleList
import io.agora.flat.Constants.Companion.DEFAULT_CALLING_CODE
import io.agora.flat.data.model.Country
import io.agora.flat.util.JsonUtils
import java.util.Locale

object CallingCodeManager {

    data class AllCountry(
        val zh: List<Country>,
        val en: List<Country>,
    )

    private var allCountries: AllCountry? = null

    val countries: List<Country>
        get() = when (LanguageManager.currentLocale().language) {
            Locale("zh").language -> allCountries?.zh ?: emptyList()
            else -> allCountries?.en ?: emptyList()
        }

    fun init(application: Application) {
        val context = application.applicationContext
        context.assets.open("countries.json").use {
            it.bufferedReader().use { reader ->
                allCountries = JsonUtils.fromJson(reader.readText(), AllCountry::class.java)
            }
        }
    }

    fun getDefaultCC(): String {
        return countries.find { it.code.equals(getSystemDefaultLocale().country, true) }?.cc ?: DEFAULT_CALLING_CODE
    }

    fun getCC(locale: Locale): String {
        return countries.find { it.code.equals(locale.country, true) }?.cc ?: DEFAULT_CALLING_CODE
    }

    private fun getSystemDefaultLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            Locale.getDefault()
        }
    }
}