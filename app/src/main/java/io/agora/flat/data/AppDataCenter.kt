package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.BuildConfig
import io.agora.flat.data.model.UserInfo
import javax.inject.Inject

/**
 * 提供App级别的KV存储
 */

class AppDataCenter @Inject constructor(@ApplicationContext context: Context) {
    private val store: SharedPreferences =
        context.getSharedPreferences("global_kv_data", Context.MODE_PRIVATE)
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

    companion object {
        const val KEY_LOGIN_TOKEN = "key_login_token"

        const val KEY_LOGIN_USER_INFO = "key_login_user_info"
    }
}

class MockData() {
    companion object {
        // const val LOGGEDIN_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6IjcyMmY3ZjZkLWNjMGYtNGU2My1hNTQzLTQ0NmEzYjdiZDY1OSIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE3ODc0NTMxLCJleHAiOjE2MjAzODAxMzEsImlzcyI6ImZsYXQtc2VydmVyIn0.v3SaPYMLRGqlWyOeLHFWTm4i3KVyjpo7QyX8SpewNq0"
        // yangliu
        const val LOGGEDIN_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6ImZiNDhmYTM3LWFhOWItNGI1Zi04MTAwLTllNmMzYmQ0MTExNSIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE4NDc1MDE3LCJleHAiOjE2MjA5ODA2MTcsImlzcyI6ImZsYXQtc2VydmVyIn0.srW5y4TstK5Y_BrAQz2JT9I_HZXT7zz5cXa7zpqJEEA"
        const val USER_UUID = "fb48fa37-aa9b-4b5f-8100-9e6c3bd41115"
    }

    private val gson = Gson()

    fun isMockEnable(): Boolean {
        return BuildConfig.DEBUG;
    }

    fun isUserLoggedIn(): Boolean {
        return true
    }

    fun getUserInfo(): UserInfo {
        val userInfoJson =
            "{\"status\":0,\"data\":{\"name\":\"一生何求\",\"sex\":\"Woman\",\"avatar\":\"https://thirdwx.qlogo.cn/mmopen/vi_32/lgQPSTL3aTUKbFVWibYLibg1fRQaOhKs7oCXSbHYYvG3ozI3AF47vGngqiaXHypachKM2h0VAcMXT8FXnT9iaFRKAw/132\",\"userUUID\":\"fb48fa37-aa9b-4b5f-8100-9e6c3bd41115\",\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6ImZiNDhmYTM3LWFhOWItNGI1Zi04MTAwLTllNmMzYmQ0MTExNSIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE4NDc1MDE3LCJleHAiOjE2MjA5ODA2MTcsImlzcyI6ImZsYXQtc2VydmVyIn0.srW5y4TstK5Y_BrAQz2JT9I_HZXT7zz5cXa7zpqJEEA\"}}"
        return gson.fromJson(userInfoJson, UserInfo::class.java)
    }

    fun getToken(): String? {
        return LOGGEDIN_TOKEN
    }
}