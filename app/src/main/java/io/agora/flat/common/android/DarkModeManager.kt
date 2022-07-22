package io.agora.flat.common.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import io.agora.flat.R

object DarkModeManager {
    enum class Mode(val type: String, @StringRes val display: Int) {
        Auto("auto", R.string.dark_mode_auto),
        Light("light", R.string.dark_mode_light),
        Dark("dark", R.string.dark_mode_dark),
        ;

        companion object {
            fun of(mode: String?): Mode {
                return values().find { it.type == mode } ?: Auto
            }
        }
    }

    private const val KEY_DARK_MODE = "key_dark_mode"

    private var store: SharedPreferences? = null

    fun init(application: Application) {
        store = application.getSharedPreferences("flat_config", Context.MODE_PRIVATE)
        setDarkMode(current())
    }

    fun current(): Mode {
        return Mode.of(store?.getString(KEY_DARK_MODE, Mode.Auto.type))
    }

    fun update(mode: Mode) {
        setDarkMode(mode)
        store?.edit(commit = true) {
            putString(KEY_DARK_MODE, mode.type)
        }
    }

    private fun setDarkMode(mode: Mode) {
        AppCompatDelegate.setDefaultNightMode(when (mode) {
            Mode.Auto -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            Mode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Mode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        })
    }
}