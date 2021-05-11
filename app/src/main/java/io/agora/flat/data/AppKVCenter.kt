package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.data.model.UserInfo
import javax.inject.Inject

/**
 * 提供App级别的KV存储
 */
class AppKVCenter @Inject constructor(@ApplicationContext context: Context) {
    private val store: SharedPreferences =
        context.getSharedPreferences("flat_kv_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mockData = MockData()

    // region user
    fun setLogout() {
        store.edit().apply {
            putString(KEY_LOGIN_TOKEN, null)
        }.apply()
    }

    fun isUserLoggedIn(): Boolean {
        if (mockData.isMockEnable()) {
            return mockData.isUserLoggedIn()
        }
        return !store.getString(KEY_LOGIN_TOKEN, null).isNullOrEmpty()
    }

    fun setToken(token: String) {
        store.edit().apply {
            putString(KEY_LOGIN_TOKEN, token)
        }.apply()
    }

    fun getToken(): String? {
        if (mockData.isMockEnable()) {
            return mockData.getToken()
        }
        return store.getString(KEY_LOGIN_TOKEN, null)
    }

    fun setUserInfo(userInfo: UserInfo) {
        store.edit().apply {
            putString(KEY_LOGIN_USER_INFO, gson.toJson(userInfo))
        }.apply()
    }

    fun getUserInfo(): UserInfo? {
        if (mockData.isMockEnable()) {
            return mockData.getUserInfo()
        }

        val userInfoJson = store.getString(KEY_LOGIN_USER_INFO, null)
        if (userInfoJson.isNullOrBlank())
            return null
        return gson.fromJson(userInfoJson, UserInfo::class.java)
    }
    // endregion

    // TODO 临时全局变量的存放
    fun setAuthUUID(authUUID: String) {
        store.edit().apply {
            putString(KEY_AUTH_UUID, authUUID)
        }.apply()
    }

    fun getAuthUUID(): String {
        return store.getString(KEY_AUTH_UUID, "") ?: ""
    }

    companion object {
        const val KEY_LOGIN_TOKEN = "key_login_token"

        const val KEY_LOGIN_USER_INFO = "key_login_user_info"

        const val KEY_AUTH_UUID = "key_auth_uuid"
    }

    class MockData {
        companion object {
            const val LOGGED_IN_TOKEN =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6ImM5NDI3MGE0LTJlNjktNDUzNS1iZWMwLTlhMmM2NTQ4YTcyYiIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE5NjY4NTQ1LCJleHAiOjE2MjIxNzQxNDUsImlzcyI6ImZsYXQtc2VydmVyIn0.vXNn1mGP7ut-so9xafQF0vkEGUrqS2VAbbkIKo_EbV4"
            var mockEnable = false
        }

        private val gson = Gson()

        fun isMockEnable(): Boolean {
            return mockEnable
        }

        fun isUserLoggedIn(): Boolean {
            return true
        }

        fun getUserInfo(): UserInfo {
            val userInfoJson =
                "{\"name\":\"一生何求\",\"avatar\":\"https://thirdwx.qlogo.cn/mmopen/vi_32/GDpXrJ10nia06eLjbh7BJYyRpXq2jJmNEia2CztyayjWD63eX9RkIa9iaDMOZV8VZ7bLANibm33wicFmutTomYvcicuQ/132\",\"userUUID\":\"c94270a4-2e69-4535-bec0-9a2c6548a72b\",\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6ImM5NDI3MGE0LTJlNjktNDUzNS1iZWMwLTlhMmM2NTQ4YTcyYiIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE5NjY4NTQ1LCJleHAiOjE2MjIxNzQxNDUsImlzcyI6ImZsYXQtc2VydmVyIn0.vXNn1mGP7ut-so9xafQF0vkEGUrqS2VAbbkIKo_EbV4\"}"
            return gson.fromJson(userInfoJson, UserInfo::class.java)
        }

        fun getToken(): String {
            return LOGGED_IN_TOKEN
        }
    }
}