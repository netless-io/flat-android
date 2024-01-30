package io.agora.flat.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.common.board.DeviceState
import io.agora.flat.data.model.LoginHistory
import io.agora.flat.data.model.LoginHistoryItem
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.model.UserInfoWithToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提供App级别的KV存储
 */
@Singleton
class AppKVCenter @Inject constructor(@ApplicationContext context: Context) {
    private val store: SharedPreferences = context.getSharedPreferences("flat_kv_data", Context.MODE_PRIVATE)
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

    fun addLoginHistoryItem(item: LoginHistoryItem) {
        val updatedItems = (listOf(item) + getLoginHistory().items).take(5)
        store.edit().apply {
            putString(KEY_LOGIN_HISTORY, gson.toJson(LoginHistory(updatedItems)))
        }.apply()
    }

    fun getLastLoginHistoryItem(): LoginHistoryItem? {
        return getLoginHistory().items.firstOrNull()
    }

    private fun getLoginHistory(): LoginHistory {
        val json = store.getString(KEY_LOGIN_HISTORY, null) ?: return LoginHistory()
        return gson.fromJson(json, LoginHistory::class.java)
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

    fun setProtocolAgreed(agreed: Boolean) {
        store.edit().apply {
            putBoolean(KEY_AGREEMENT_GLOBAL_AGREED, agreed)
        }.apply()
    }

    fun isProtocolAgreed(): Boolean {
        return store.getBoolean(KEY_AGREEMENT_GLOBAL_AGREED, false)
    }

    fun setNetworkAcceleration(enable: Boolean) {
        store.edit().apply {
            putBoolean(KEY_NETWORK_ACCELERATION, enable)
        }.apply()
    }

    fun isNetworkAcceleration(): Boolean {
        // default close fpa
        return store.getBoolean(KEY_NETWORK_ACCELERATION, false)
    }

    fun setUseProjectorConvertor(enable: Boolean) {
        store.edit().apply {
            putBoolean(KEY_PROJECTOR_CONVERTOR, enable)
        }.apply()
    }

    fun useProjectorConvertor(): Boolean {
        return store.getBoolean(KEY_PROJECTOR_CONVERTOR, true)
    }

    fun getLastCancelUpdate(): Long {
        return store.getLong(KEY_LAST_CANCEL_UPDATE, 0)
    }

    fun setLastCancelUpdate(timeMillis: Long) {
        store.edit().apply {
            putLong(KEY_LAST_CANCEL_UPDATE, timeMillis)
        }.apply()
    }

    fun getSessionId(): String {
        return store.getString(KEY_SESSION_ID, "") ?: ""
    }

    fun updateSessionId(sessionId: String) {
        store.edit().apply {
            putString(KEY_SESSION_ID, sessionId)
        }.apply()
    }

    fun getDeviceStatePreference(): DeviceState {
        val preferenceJson = store.getString(KEY_DEVICE_STATE, null)
        return if (preferenceJson == null) {
            DeviceState(camera = false, mic = true);
        } else {
            gson.fromJson(preferenceJson, DeviceState::class.java)
        }
    }

    fun setDeviceStatePreference(deviceState: DeviceState) {
        store.edit().apply {
            putString(KEY_DEVICE_STATE, gson.toJson(deviceState))
        }.apply()
    }

    fun getJoinEarly(): Int {
        return store.getInt(KEY_SERVER_JOIN_EARLY, 5)
    }

    fun setJoinEarly(joinEarly: Int) {
        store.edit().apply {
            putInt(KEY_SERVER_JOIN_EARLY, joinEarly)
        }.apply()
    }

    companion object {
        const val KEY_LOGIN_TOKEN = "key_login_token"

        const val KEY_LOGIN_USER_INFO = "key_login_user_info"

        const val KEY_AUTH_UUID = "key_auth_uuid"

        const val KEY_AGREEMENT_GLOBAL_AGREED = "key_agreement_global_agreed"

        const val KEY_NETWORK_ACCELERATION = "key_network_acceleration"

        const val KEY_PROJECTOR_CONVERTOR = "key_convertor_projector"

        const val KEY_LAST_CANCEL_UPDATE = "key_last_cancel_update"

        const val KEY_SESSION_ID = "key_session_id"

        const val KEY_DEVICE_STATE = "key_device_state"

        const val KEY_LOGIN_HISTORY = "key_login_history"

        const val KEY_SERVER_JOIN_EARLY = "key_server_join_early"
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