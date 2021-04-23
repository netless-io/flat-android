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

class MockData {
    companion object {
        const val LOGGED_IN_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6ImFmZWU4Njk1LTc4N2EtNDIyMS05NzU2LWMzYjhiOWIxZDJjNiIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE5MTU4NzYzLCJleHAiOjE2MjE2NjQzNjMsImlzcyI6ImZsYXQtc2VydmVyIn0.Rt9b1AjPfgWAFK6bsSpQHNnV7Ye6m7Du-dzP6k7Gmtw"
    }

    private val gson = Gson()

    fun isMockEnable(): Boolean {
        return true;
    }

    fun isUserLoggedIn(): Boolean {
        return true
    }

    fun getUserInfo(): UserInfo {
        val userInfoJson =
            "{\"name\":\"一生何求\",\"avatar\":\"https://thirdwx.qlogo.cn/mmopen/vi_32/zscTBLOCMOXw8XYOwToAGcE2utFMomr6CPBA9U5USgEXS532uYMibfrceS4hFhxEWSL1xYcrzTsRLHubc9Tauug/132\",\"userUUID\":\"afee8695-787a-4221-9756-c3b8b9b1d2c6\",\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6ImFmZWU4Njk1LTc4N2EtNDIyMS05NzU2LWMzYjhiOWIxZDJjNiIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjE5MTU4NzYzLCJleHAiOjE2MjE2NjQzNjMsImlzcyI6ImZsYXQtc2VydmVyIn0.Rt9b1AjPfgWAFK6bsSpQHNnV7Ye6m7Du-dzP6k7Gmtw\"}"
        return gson.fromJson(userInfoJson, UserInfo::class.java)
    }

    fun getToken(): String {
        return LOGGED_IN_TOKEN
    }
}