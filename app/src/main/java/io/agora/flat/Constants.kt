package io.agora.flat

class Constants {
    companion object {
        const val WX_APP_ID = "wx09437693798bc108"

        const val NETLESS_APP_IDENTIFIER = "cFjxAJjiEeuUQ0211QCRBw/mO9uJB_DiCIqug"

        const val AGORA_APP_ID = "931b86d6781e49a2a255db4ce6e8e804"

        const val OSS_ACCESS_KEY_ID = "LTAI5t9Gb6tzQzzLmB6cTVf7"
    }

    object URL {
        const val Service = "https://flat.whiteboard.agora.io/service.html"
        const val Privacy = "https://flat.whiteboard.agora.io/privacy.html"
    }

    object IntentKey {
        const val ROOM_UUID = "room_uuid"
        const val PERIODIC_UUID = "periodic_uuid"
        const val ROOM_INFO = "room_info"
        const val ROOM_PLAY_INFO = "room_play_info"

        const val URL = "url"
        const val TITLE = "title"
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
}