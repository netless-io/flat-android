package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.model.UserInfoWithToken
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
            var mockEnable = false
            const val userInfoJson = """
                {"name":"冯利斌","avatar":"https://thirdwx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTKUtPsvnxiaQtoHwaFPErfOrq1uN6wQ5UoMk7y2pPXcEibbVgTWBxeRrV80b4HkuJNB8o1STgaDXicFQ/132","userUUID":"3e092001-eb7e-4da5-a715-90452fde3194","token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyVVVJRCI6IjNlMDkyMDAxLWViN2UtNGRhNS1hNzE1LTkwNDUyZmRlMzE5NCIsImxvZ2luU291cmNlIjoiV2VDaGF0IiwiaWF0IjoxNjM2NDQ3Njg0LCJleHAiOjE2Mzg5NTMyODQsImlzcyI6ImZsYXQtc2VydmVyIn0.OvZCVBPPWDSUX8vwfTOSl81gnYRquLSVP2s5Xnslyrc"}
            """
        }

        private val gson = Gson()

        fun isMockEnable(): Boolean {
            return mockEnable
        }

        fun isUserLoggedIn(): Boolean {
            return true
        }

        fun getUserInfo(): UserInfo {
            return gson.fromJson(userInfoJson, UserInfo::class.java)
        }

        fun getToken(): String {
            val withToken = gson.fromJson(userInfoJson, UserInfoWithToken::class.java)
            return withToken.token
        }
    }
}