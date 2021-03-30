package com.agora.netless.flat.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 提供App级别的KV存储
 */
class AppDataCenter(context: Context) {
    private val store: SharedPreferences =
        context.getSharedPreferences("global_kv_data", Context.MODE_PRIVATE)

    // region user
    fun setUserLoggedIn(loggedIn: Boolean) {
        store.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, loggedIn)
        }.apply()
    }

    fun isUserLoggedIn(defaultValue: Boolean = false): Boolean {
        return store.getBoolean(KEY_IS_LOGGED_IN, defaultValue)
    }
    // endregion

    companion object {
        const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    }
}