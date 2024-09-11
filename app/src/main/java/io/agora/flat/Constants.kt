package io.agora.flat

class Constants {
    companion object {
        const val UPLOAD_TAG_CLOUD = "cloud"

        const val UPLOAD_TAG_TAKE_PHOTO = "take_photo"
    }

    object URL {
        const val Registration = "https://beian.miit.gov.cn"

        const val Libraries = "https://www.flat.apprtc.cn/privacy-extra/libraries.html"
    }

    object IntentKey {
        const val ROOM_UUID = "room_uuid"
        const val PERIODIC_UUID = "periodic_uuid"
        const val ROOM_PLAY_INFO = "room_play_info"
        const val ROOM_QUICK_START = "room_quick_start"
        const val CLOUD_FILE = "cloud_file"

        const val URL = "url"
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val COUNTRY = "country"

        const val FROM = "from"

        const val PHONE: String = "phone"
        const val CALLING_CODE: String = "calling_code"
    }

    object Login {
        const val AUTH_SUCCESS = 0
        const val AUTH_DENIED = 1
        const val AUTH_CANCEL = 2
        const val AUTH_ERROR = 3

        const val KEY_LOGIN_STATE = "login_state"
        const val KEY_LOGIN_RESP = "login_resp"
        const val KEY_ERROR_CODE = "error_code"
        const val KEY_ERROR_MESSAGE = "error_message"
    }

    object From {
        const val UserSecurity = "user_security"
        const val Login = "third_party_login"
    }
}